package controllers.modules2.framework.procs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import models.message.StreamModule;

import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.DNegationProcessor;
import controllers.modules2.framework.chain.FTranslatorValuesToJson;
import controllers.modules2.framework.http.HttpStatus;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

public abstract class ProcessorSetupAbstract implements ProcessorSetup {

	protected ProcessorSetup parent;
	protected Path params;
	private Map<String, String> options;
	protected ProcessorSetup child;
	protected String timeColumn;
	protected String valueColumn;
	
	@Inject
	private RawProcessorFactory factory;
	@Inject
	private Provider<EngineProcessor> engines;
	@Inject
	private Provider<DNegationProcessor> processors;
	@Inject
	private Provider<BufferNode> buffers;
	
	@Override
	public String toString() {
		if(child == null)
			return getClass().getSimpleName();
		return getClass().getSimpleName()+"-"+child;
	}

	@Override
	public void setChild(ProcessorSetup child) {
		this.child = child;
	}

	@Override
	public void start(VisitorInfo visitor) {
		child.start(visitor);
	}

	@Override
	public String getUrl() {
		return params.getPreviousPath();
	}

	@Override
	public ProcessorSetup createTree(StreamModule stream, VisitorInfo visitor) {
		String moduleName = stream.getModule();
		Map<String, String> options = stream.getParams();

		RawProcessorFactory.threadLocal.set(moduleName);
		child = factory.get();
		if(child == null) //Needs to be removed so external modules can work
			throw new DatabusBadRequest("Processor="+moduleName+" does not exist at this time");

		if(child.getSourceDirection() != Direction.PULL)
			throw new IllegalStateException("Can't wire in child="+child+" as he is not a PullProcessor");

		if(child instanceof EndOfChain) {
			Request request1 = Request.current();
			String val = request1.params.get("reverse");
			if("true".equalsIgnoreCase(val)) {
				DNegationProcessor proc = processors.get();
				proc.initModule(this, visitor, options);
				child.initModule(proc, visitor, options);
				ProcessorSetup grandChild = child;
				proc.setChild(grandChild);
				child = proc;
			}
		} else {
			child.initModule(this, visitor, options);
			StreamModule childInfo = null;
			if(stream.getStreams().size() > 0) {
				childInfo = stream.getStreams().get(0);
				child.createTree(childInfo, visitor);
			}
		}
		return child;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup useThisChild, boolean alreadyAddedInverter) {
		String[] pieces = path.split("/");
		String moduleName = pieces[0];
		HashMap<String, String> childsOptions = new HashMap<String, String>();
		if (StringUtils.contains(moduleName, "(")) {
			String optionString = StringUtils.substringBetween(moduleName, "(", ")");
			for (String option: optionString.split(",")) 
				childsOptions.put(StringUtils.substringBefore(option, "="), StringUtils.substringAfter(option, "="));
			moduleName = StringUtils.substringBefore(moduleName, "(");
		}

		child = useThisChild;
		if(child == null) {
			RawProcessorFactory.threadLocal.set(moduleName);
			child = factory.get();
			if(child == null) //Needs to be removed so external modules can work
				throw new DatabusBadRequest("Processor="+moduleName+" does not exist at this time");
		}

		if(child.getSourceDirection() == Direction.NONE)
			throw new IllegalStateException("Can't wire in child="+child+" as he has no source direction");

		ProcessorSetup grandChild = null;
		ProcessorSetupAbstract parent = this;
		if(parent.getSinkDirection() == Direction.NONE)
			throw new IllegalStateException("bug, how is parent="+parent+" have no sink direction, we can't wire in then");
		else if(parent.getSinkDirection() == Direction.EITHER)
			throw new IllegalStateException("bug, parent="+parent+" must return hist parent's direction!!! not EITHER!!");
		else if(parent.getSinkDirection() == Direction.PUSH) {
			if(child.getSourceDirection() == Direction.PULL) {
				//engine has pull as it's sink to match the child source
				//and has push source to match the push sink...
				EngineProcessor engine = engines.get();
				grandChild = child;
				child = engine;				
			}
		} else if(parent.getSinkDirection() == Direction.PULL) {
			if(child.getSourceDirection() == Direction.PUSH) {
				//here the parent reads/pulls so we need a special buffer between us and the child.
				//The child will write to the buffer(shutting off if full), the parent reads from the buffer
				BufferNode buffer = buffers.get();
				grandChild = child;
				child = buffer;				
			}
		} else
			throw new IllegalStateException("bug, should not end up here, unknow direction="+parent.getSinkDirection());

		if(child instanceof EndOfChain && !alreadyAddedInverter) {
			alreadyAddedInverter = true;
			Request request1 = Request.current();
			String val = request1.params.get("reverse");
			if("true".equalsIgnoreCase(val)) {
				DNegationProcessor proc = processors.get();
				grandChild = child;
				child = proc;
			}
		}

		String newPath = child.init(path, this, visitor, childsOptions);
		child.createPipeline(newPath, visitor, grandChild, alreadyAddedInverter);
		return child;
	}

	public Path parsePath(String path, VisitorInfo visitor) {
		int numParams = getNumParams();
		List<String> result = new ArrayList<String>();
		String[] pieces = path.split("/");
		int i = 1;
		for(; i < numParams+1; i++) {
			result.add(pieces[i]);
		}
		String newPath ="";
		for(int n = i; n < pieces.length; n++) {
			newPath += pieces[n];
			if(n+1 < pieces.length)
				newPath+="/";
		}
		
		
		Long start = null;
		Long end;
		String to   = pieces[pieces.length-1];
		try {
			end = Long.parseLong(to);
		} catch(NumberFormatException e) {
			end = null;
		}

		if(pieces.length >= 2) {
			String from = pieces[pieces.length-2];
			try {
				start = Long.parseLong(from);
			} catch(NumberFormatException e) {
				//there is no times, so let's create our own
				start = null;
			}
		}		
		return new Path(result, path, newPath, start, end, visitor.isReversed());
	}
	
	protected int getNumParams() {
		return 0;
	}
	
	@Override
	public void initModule(ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		this.options = options;
		this.parent = nextInChain;
		timeColumn = "time";
		valueColumn = "value";
		if (options.containsKey("timeColumn"))
			timeColumn=options.get("timeColumn");
		if (options.containsKey("valueColumn"))
			valueColumn=options.get("valueColumn");
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		Path pathInfo = parsePath(path, visitor);
		this.options = options;
		this.params = pathInfo;
		this.parent = nextInChain;
		//for now, hard code these until we know how we will pass them in
		timeColumn = "time";
		valueColumn = "value";
		if (options.containsKey("timeColumn"))
			timeColumn=options.get("timeColumn");
		if (options.containsKey("valueColumn"))
			valueColumn=options.get("valueColumn");
		
		return pathInfo.getLeftOverPath();
	}

	protected BigDecimal getValueEvenIfNull(TSRelational row) {
		Object obj = row.get(valueColumn);
		if(obj == null)
			return null;
		else if(obj instanceof BigDecimal)
			return (BigDecimal) obj;

		BigInteger bigInt = (BigInteger) obj;
		BigDecimal newVal = new BigDecimal(bigInt);
		return newVal;
	}
	
	protected void setValue(TSRelational row, BigDecimal val) {
		row.put(valueColumn, val);
	}
	
	/**
	 * NOTE: Feel free to override that if you want to process nulls otherwise this helps us
	 * immediately cut out of processing a null row in this module
	 * @param row
	 * @return
	 */
	protected long getTime(TSRelational row) {
		BigInteger strVal = (BigInteger) row.get(timeColumn);
		if(strVal == null)
			throw new SuccessfulAbort("value is null, most modules can't handle that");
		return strVal.longValue();
	}
	
	protected void setTime(TSRelational row, long time) {
		row.put(timeColumn, new BigInteger(""+time));
	}
	
	protected long parseLong(String param, String msg) {
		try {
			return Long.parseLong(param);
		} catch(NumberFormatException e) {
			throw new BadRequest(msg);
		}
	}
	protected int parseInteger(String param, String msg) {
		try {
			return Integer.parseInt(param);
		} catch(NumberFormatException e) {
			throw new BadRequest(msg);
		}
	}
	protected float parseFloat(String param, String msg) {
		try {
			return Float.parseFloat(param);
		} catch(NumberFormatException e) {
			throw new BadRequest(msg);
		}
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}
	
	@Override
	public List<String> getAggregationList() {
		List<String> aggregationList;
		if (parent != null) 
			aggregationList = parent.getAggregationList();
		else 
			aggregationList = new ArrayList<String>();
		return aggregationList;
	}
}
