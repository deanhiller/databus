package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import models.message.ChartVarMeta;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class RangeCleanProcessor extends PushOrPullProcessor {

	private static final String MAX_KEY = "max";
	private static final String MIN_KEY = "min";
	private BigDecimal min;
	private BigDecimal max;

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, false, "Range Clean", "Data Cleaning");

	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Min Value");
		meta1.setNameInJavascript(MIN_KEY);
		meta1.setRequired(true);
		meta1.setHelp("Any value below min value is dropped.  Values matching min value are kept");
		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Max Value");
		meta.setNameInJavascript(MAX_KEY);
		meta.setRequired(true);
		meta.setHelp("Any value above max value is dropped.  Values matching max value are kept");
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		parameterMeta.put(meta.getNameInJavascript(), meta);
		
		metaInfo.setDescription("This module drops all data outside the specified min and max values and sends the rest of the data to the next module");
	}
	
	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}

	@Override
	protected int getNumParams() {
		return 2;
	}
	
	
	@Override
	public void initModule(Map<String, String> options, long start, long end) {
		super.initModule(options, start, end);
		String minStr = options.get(MIN_KEY);
		String maxStr = options.get(MAX_KEY);
		this.min = parseBigDecimal(minStr, "error");
		this.max = parseBigDecimal(maxStr, "error");	
	}

	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		String minStr = params.getParams().get(0);
		String maxStr = params.getParams().get(1);
		String msg = "module url /rangeclean/{min}/{max} must be parsable as a BigDecimal for min and max and was not in the proper format";
		this.min = parseBigDecimal(minStr, msg);
		this.max = parseBigDecimal(maxStr, msg);
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
