package controllers.modules2;

import java.math.BigDecimal;

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

public class RangeCleanProcessor extends PushOrPullProcessor {

	private BigDecimal min;
	private BigDecimal max;

	@Override
	protected int getNumParams() {
		return 2;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor) {
		String newPath = super.init(pathStr, nextInChain, visitor);
		String minStr = params.getParams().get(0);
		String maxStr = params.getParams().get(1);
		String msg = "module url /rangeclean/{min}/{max} must be passed a long for min and max and was not passed that";
		long min = parseLong(minStr, msg);
		long max = parseLong(maxStr, msg);
		this.min = new BigDecimal(min);
		this.max = new BigDecimal(max);
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational tv) {
		BigDecimal val = getValueEvenIfNull(tv);
		if(val == null)
			return tv;

		if(val.compareTo(min) < 0 || val.compareTo(max) > 0)
			return null;
		
		return tv;
	}

}
