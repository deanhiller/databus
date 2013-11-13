package controllers.modules2.framework.procs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.message.StreamModule;

import org.apache.commons.lang.StringUtils;

import com.netflix.astyanax.connectionpool.exceptions.IsRetryableException;

import play.mvc.Http.Request;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.DNegationProcessor;

public abstract class ProcessorSetupContainer extends ProcessorSetupAbstract {

	protected List<ProcessorSetup> children = new ArrayList<ProcessorSetup>();
	protected ProcessorSetup parent;
	
	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		this.parent = nextInChain;
		return super.init(path, nextInChain, visitor, options);
	}
	
	@Override
	public void createTree(ProcessorSetup parent, StreamModule thisNodeInfo, VisitorInfo visitor) {
		this.parent = parent;
		Request request1 = Request.current();
		String val = request1.params.get("reverse");
		boolean isReversed = "true".equalsIgnoreCase(val);
		
		for(StreamModule childInfo : thisNodeInfo.getStreams()) {
			String moduleName = childInfo.getModule();
	
			RawProcessorFactory.threadLocal.set(moduleName);
			ProcessorSetup child = factory.get();
			if(child == null) //Needs to be removed so external modules can work
				throw new DatabusBadRequest("Processor="+moduleName+" does not exist at this time");
	
			if(child.getSourceDirection() != Direction.PULL && child.getSourceDirection() != Direction.EITHER)
				throw new IllegalStateException("Can't wire in child="+child+" as he is not a PullProcessor");
	
			if(child instanceof EndOfChain && isReversed) {
				DNegationProcessor proc = processors.get();
				proc.createTree(this, new StreamModule(), visitor);
				child.createTree(proc, childInfo, visitor);
				ProcessorSetup grandChild = child;
				proc.setChild(grandChild);
				child = proc;
			} else {
				child.createTree(this, childInfo, visitor);
			}

			children.add(child);
		}
		
		
		
		String startStr = Request.current().params.get("start");
		String endStr = Request.current().params.get("end");
		long start = Long.MIN_VALUE+1;
		long end = System.currentTimeMillis();
		if(startStr != null)
			start = Long.parseLong(startStr);
		if(endStr != null)
			end = Long.parseLong(endStr);

		if(isReversed) {
			long temp = start;
			start = -end;
			end = -temp;
		}

		Map<String, String> modOptions2 = thisNodeInfo.getParams();
		initModule(modOptions2, start, end);
	}

	public void setChild(ProcessorSetup child) {
		children.add(child);
	}

	@Override
	public void start(VisitorInfo visitor) {
		for(ProcessorSetup child : children)
			child.start(visitor);
		
		if(children.size() == 1) {
			ProcessorSetup processor = getSingleChild();
			RowMeta meta = processor.getRowMeta();
			if(meta != null) {
				String timeCol = meta.getTimeColumn();
				String valCol = meta.getValueColumn();
				if(timeCol != null)
					timeColumn = timeCol;
				if(valCol != null)
					valueColumn = valCol;
			}
		}
	}

	public RowMeta getRowMeta() {
		if(children.size() == 1)
			return getSingleChild().getRowMeta();
		throw new RuntimeException("Class="+getClass()+" needs to override getRowMeta");
	}

	@Override
	public String toString() {
		if(children.size() == 0)
			return getClass().getSimpleName();
		else if(children.size() == 1)
			return getClass().getSimpleName()+"-"+children.get(0);

		String msg = getClass().getSimpleName()+"-|";
		for(ProcessorSetup s : children) {
			msg += s+"|";
		}
		return msg;
	}

	public ProcessorSetup getSingleChild() {
		return children.get(0);
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

		ProcessorSetup child = useThisChild;
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
		children.add(child);
		return child;
	}
	
	@Override
	public List<String> getAggregationList() {
		if(parent != null)
			return parent.getAggregationList();
		return new ArrayList<String>();
	}
	
	protected Long fetchLong(String key, Map<String, String> options) {
		String val = options.get(key);
		return Long.parseLong(val);
	}

}
