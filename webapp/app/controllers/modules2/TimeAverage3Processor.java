package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import models.message.ChartVarMeta;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EmptyWindowProcessor2;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;

public class TimeAverage3Processor extends EmptyWindowProcessor2 {

	private int numberOfPoints = 0;
	private BigDecimal total;
	
	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, true, "Time Average");
	
	public static String INTERVAL_NAME = "interval";
	public static String OFFSET_NAME = "epochOffset";
	
	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Interval");
		meta1.setNameInJavascript(INTERVAL_NAME);
		meta1.setDefaultValue("60000");
		meta1.setClazzType(Integer.class);
		ChartVarMeta meta2 = new ChartVarMeta();
		meta2.setLabel("Epoch Offset");
		meta2.setDefaultValue("0");
		meta2.setNameInJavascript(OFFSET_NAME);
		meta2.setClazzType(Integer.class);
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		parameterMeta.put(meta2.getNameInJavascript(), meta2);
		
		metaInfo.setDescription("This module takes data from the source module and does a time average of each window of data in the specific time interval");
	}

	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}

	protected int getNumParams() {
		return 2;
	}
	
	@Override
	public void initModule(Map<String, String> options, long start, long end) {
		super.initModule(options, start, end);
		long interval = fetchLong(INTERVAL_NAME, options);
		long epochOffset = fetchLong(OFFSET_NAME, options);
		super.initEmptyParser(start, end, interval, epochOffset);
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		String msg = "The url /timeaverageV2/{interval}/{epochOffset} must have a long as the interval but it was not a long";
		long interval = parseLong(params.getParams().get(0), msg);
		String msg2 = "The url /timeaverageV2/{interval}/{epochOffset} must have a long as the epochOffset but it was not a long";
		long epochOffset = parseLong(params.getParams().get(1), msg2);
		super.initEmptyParser(params.getStart(), params.getEnd(), interval, epochOffset);
		return newPath;
	}
	
	@Override
	protected void incomingTimeValue(long time, TSRelational row) {
		BigDecimal value = getValueEvenIfNull(row);
		if(value == null)
			return;
		
		if(total == null) 
			total = BigDecimal.ZERO;
		total = total.add(value);
		numberOfPoints++;
	}

	@Override
	protected TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow) {
		TSRelational r = new TSRelational(timeColumn, valueColumn);
		setTime(r, endOfWindow);
		if(total != null) {
			BigDecimal average = total.divide(new BigDecimal(numberOfPoints), 10, RoundingMode.HALF_UP);
			setValue(r, average);
		}
		
		//reset the state
		numberOfPoints = 0;
		total = null;
		return r;
	}

}
