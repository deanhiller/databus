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
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

public abstract class ProcessorSetupAbstract extends PlayPlugin implements ProcessorSetup {
//public abstract class ProcessorSetupAbstract implements ProcessorSetup {

	protected Path params;
	private Map<String, String> options;
	protected String timeColumn;
	protected String valueColumn;
	
	@Inject
	protected RawProcessorFactory factory;
	@Inject
	protected Provider<EngineProcessor> engines;
	@Inject
	protected Provider<DNegationProcessor> processors;
	@Inject
	protected Provider<BufferNode> buffers;
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String getUrl() {
		if(params == null)
			return null;
		return params.getPreviousPath();
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
	
	public void initModule(Map<String, String> options, long start, long end) {
		this.options = options;
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
	
	protected BigDecimal parseBigDecimal(String param, String msg) {
		try {
			return new BigDecimal(param);
		} catch(NumberFormatException e) {
			throw new BadRequest(msg);
		}
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
	
}
