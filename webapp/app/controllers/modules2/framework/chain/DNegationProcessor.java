package controllers.modules2.framework.chain;

import java.math.BigInteger;
import java.util.Map;

import play.mvc.Http.Request;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EngineProcessor;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class DNegationProcessor extends PushOrPullProcessor {

	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		super.init(path, nextInChain, visitor, options);
		//NOTE: only different from super.init is we return the same path that is passed in to us...
		return path;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational row) {
		BigInteger time = getTimeBigInt(row);
		row.put(timeColumn, time.negate());
		return row;
	}
	
	protected BigInteger getTimeBigInt(TSRelational row) {
		BigInteger strVal = (BigInteger) row.get(timeColumn);
		if(strVal == null)
			throw new SuccessfulAbort("value is null, most modules can't handle that");
		return strVal;
	}

}
