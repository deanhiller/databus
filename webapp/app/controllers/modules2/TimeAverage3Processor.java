package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.message.ChartVarMeta;

import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EmptyWindowProcessor2;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;

public class TimeAverage3Processor extends EmptyWindowProcessor2 {

	private HashMap<String, Integer> numberOfPoints = new HashMap<String, Integer>();
	private HashMap<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
		
	public static String INTERVAL_NAME = "interval";
	public static String OFFSET_NAME = "epochOffset";
	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	
	private List<String> columnsToAverage;
	private List<ColumnState> columns = new ArrayList<ColumnState>();
	private UnAveragedValueMethod unaveragedValueMethod = UnAveragedValueMethod.PREVIOUS_ROW;


	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, true, "Time Average", "Rollup");

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
		
		columnsToAverage=Arrays.asList(new String[]{valueColumn});
		String columnsToAverageString = options.get("columnsToAverage");
		if (StringUtils.isNotBlank(columnsToAverageString)) {
			columnsToAverage = Arrays.asList(StringUtils.split(columnsToAverageString, ";"));
		}
		String unaveragedValueMethodString = options.get("unaveragedValueMethod");
		if (StringUtils.equalsIgnoreCase("nearest", unaveragedValueMethodString))
			unaveragedValueMethod = unaveragedValueMethod.NEAREST_ROW;

		return newPath;
	}
	
	@Override
	protected void incomingTimeValue(long time, TSRelational row) {
		for (String key:columnsToAverage) {
			BigDecimal value = row.getValue(key);
			if(value == null)
				continue;
			
			if(totals.get(key) == null) 
				totals.put(key, BigDecimal.ZERO);
			if(numberOfPoints.get(key) == null) 
				numberOfPoints.put(key, 0);
			totals.put(key,totals.get(key).add(value));
			numberOfPoints.put(key, numberOfPoints.get(key)+1);
		}
		
	}

	@Override
	protected TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow, long interval) {
		TSRelational r = new TSRelational();
		long time = startOfWindow + (interval/2);
		
		setTime(r, time);
		
		for (String key:columnsToAverage) {
			if (totals.get(key) != null) {
				BigDecimal average = totals.get(key).divide(new BigDecimal(numberOfPoints.get(key)), 10, RoundingMode.HALF_UP);
				r.put(key, average);
			}
			
			numberOfPoints.put(key, 0);
			totals.put(key, BigDecimal.ZERO);
		}
		
		return r;
	}
	
	private enum UnAveragedValueMethod {
		PREVIOUS_ROW,NEAREST_ROW
	}

}
