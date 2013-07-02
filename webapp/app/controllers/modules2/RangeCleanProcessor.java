package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class RangeCleanProcessor extends PushOrPullProcessor {

	private BigDecimal min;
	private BigDecimal max;

	@Override
	protected int getNumParams() {
		return 2;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
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
