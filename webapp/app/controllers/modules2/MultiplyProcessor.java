package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import play.mvc.results.BadRequest;

import controllers.modules.SplinesBasic;
import controllers.modules.SplinesLimitDerivative;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class MultiplyProcessor extends PushOrPullProcessor {

	private BigDecimal scalar;

	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		String path = params.getParams().get(0);
		try {
			scalar = new BigDecimal(path);
		} catch(NumberFormatException e) {
			throw new BadRequest("multiply module takes a single negative or positive integer to multiple by");
		}
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational tv) {
		BigDecimal val = getValueEvenIfNull(tv);
		if(val != null) {
			BigDecimal newVal = val.multiply(scalar);
			setValue(tv, newVal);
		}
		return tv;
	}

}
