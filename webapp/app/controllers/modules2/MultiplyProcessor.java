package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import models.message.ChartVarMeta;

import play.mvc.results.BadRequest;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class MultiplyProcessor extends PushOrPullProcessor {

	public static String SCALAR = "scalar";
	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, false, "Multiply", "Math Operations");

	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Scalar");
		meta1.setNameInJavascript(SCALAR);
		meta1.setDefaultValue("1");
		meta1.setClazzType(BigDecimal.class);
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		
		metaInfo.setDescription("This module takes the value of the child processor and multiplies it by the specified scalar.");
	}
	
	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}
	
	private BigDecimal scalar;

	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public void initModule(Map<String, String> options, long start, long end) {
		super.initModule(options, start, end);
		String scalarString = options.get(SCALAR);
		scalar = parseBigDecimal(scalarString, "error");	
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
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
