package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EmptyWindowProcessor;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;

public class RelationalSummaryProcessor extends EmptyWindowProcessor {

	private List<String> columnsToProcess;
	private Map<String, BigDecimal> valueAtBeginWindow = new HashMap<String, BigDecimal>();
	
	private Map<String, BigDecimal> min = new HashMap<String, BigDecimal>();
	private Map<String, Long> minAt = new HashMap<String, Long>();
	
	private Map<String, BigDecimal> max = new HashMap<String, BigDecimal>();
	private Map<String, Long> maxAt = new HashMap<String, Long>();
	
	private int numberOfPoints = 0;
	private Map<String, BigDecimal> total = new HashMap<String, BigDecimal>();

	@Override
	protected int getNumParams() {
		return 2;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		String longHelpMsg = "The full format for relationalsummary is .../relationalsummaryV1(columnsToSummarize=<columns>, ouputOptions=<options>)/{interval}/{epochOffset}/... ";
		longHelpMsg+="Where 'columns' is a semicolon separated list of column names in the source data, such as columnsToSummarize=column1;column2 ";
		longHelpMsg+="and optputOptions is a semicolon separated list of options to customize the output, which can be any combination of includeMax;includeMin;includeMinAt;includeMaxAt;includeAvg, ";
		longHelpMsg+="if any outputOptions are specified, only the options specified are output, if none are specified ALL output is included.  ";
		longHelpMsg+="interval is a long specifying how frequent the result rows occur, epoch offset is the offset to align with day/week/month/year, etc  ";

		String msg = "Expected format /relationalsummaryV1/{interval}/{interval} where interval must be a long value of the interval and that is missing";
		long interval = parseLong(params.getParams().get(0), msg+longHelpMsg);
		String msg2 = "The url /relationalsummaryV1/{interval}/{epochOffset} where epochOffset must be a long value, but it was not a long";
		long epochOffset = parseLong(params.getParams().get(1), msg2+longHelpMsg);
		
		columnsToProcess=Arrays.asList(new String[]{valueColumn});
		String columnsToProcessString = options.get("columnsToSummarize");
		if (StringUtils.isNotBlank(columnsToProcessString)) {
			columnsToProcess = Arrays.asList(StringUtils.split(columnsToProcessString, ";"));
		}
		
		super.initEmptyParser(params.getStart(), params.getEnd(), interval, epochOffset);
		return newPath;
	}
	
	@Override
	protected void incomingTimeValue(long time, TSRelational row) {
		
		
		for (String column:columnsToProcess) {
			BigDecimal value = (BigDecimal)row.get(column);
			if(value == null)
				return;
			
			if(min.get(column) == null) {
				initValues(time, column, value);
				return;
			}
			
			if(value.compareTo(min.get(column)) < 0)
				minAt.put(column, time);
			min.put(column, min.get(column).min(value));
	
			if(value.compareTo(max.get(column)) > 0)
				maxAt.put(column, time);
			max.put(column, max.get(column).max(value));
			
			if(total.get(column) == null) 
				total.put(column, BigDecimal.ZERO);
			total.put(column, total.get(column).add(value));
			
		}
		numberOfPoints++;
	}

	private void initValues(long time, String colname, BigDecimal value) {
		min.put(colname, value);
		max.put(colname, value);
		minAt.put(colname, time);
		maxAt.put(colname, time);
		valueAtBeginWindow.put(colname, null);
		if(startOfTheWindow == time) //we only do valueAtBeginOfWindow if the time is the first time at start of window
			valueAtBeginWindow.put(colname, value);
	}

	@Override
	protected TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow) {
		TSRelational r = new TSRelational(timeColumn, valueColumn);
		r.setTime(startOfWindow);
		if(min.size()!=0) {
			for (String column:columnsToProcess) {
				r.put(column+"_min", min.get(column)+"");
				r.put(column+"_minAt", minAt.get(column)+"");
				r.put(column+"_max", max.get(column)+"");
				r.put(column+"_maxAt", maxAt.get(column)+"");
				if(valueAtBeginWindow.get(column) != null)
					r.put(column, valueAtBeginWindow.get(column)+"");
				if(total != null && numberOfPoints!=0) {
					BigDecimal average = total.get(column).divide(new BigDecimal(numberOfPoints), 10, RoundingMode.HALF_UP);
					r.put(column+"_avg", average);
				}
				//reset the state
				total.put(column, null);
				min.put(column, null);
				max.put(column, null);
				minAt.put(column, null);
				maxAt.put(column, null);
			}
			numberOfPoints = 0;
		}
		
		return r;
	}

}
