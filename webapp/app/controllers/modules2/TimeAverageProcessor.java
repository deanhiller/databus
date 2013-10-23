package controllers.modules2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EmptyWindowProcessor;
import controllers.modules2.framework.procs.ProcessorSetup;

public class TimeAverageProcessor extends EmptyWindowProcessor {

	private int numberOfPoints = 0;
	private BigDecimal total;
	
	protected int getNumParams() {
		return 2;
	}
	
	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		String msg = "The url /timeaverageV1/{interval}/{interval} must have a long as the interval but it was not a long";
		long interval = parseLong(params.getParams().get(0), msg);
		String msg2 = "The url /timeaverageV1/{interval}/{epochOffset} must have a long as the epochOffset but it was not a long";
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
