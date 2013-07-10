package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.EmptyWindowProcessor;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;

public class MinMaxProcessor extends EmptyWindowProcessor {

	private BigDecimal valueAtBeginWindow;
	
	private BigDecimal min;
	private Long minAt;
	
	private BigDecimal max;
	private Long maxAt;

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		String msg = "After the /minmaxV1/ in the url must be a long value of the interval and that is missing";
		long interval = parseLong(params.getParams().get(0), msg);
		super.initEmptyParser(params.getStart(), params.getEnd(), interval, null);
		return newPath;
	}
	
	@Override
	protected void incomingTimeValue(long time, TSRelational row) {
		BigDecimal value = getValueEvenIfNull(row);
		if(value == null)
			return;
		
		if(min == null) {
			initValues(time, value);
			return;
		}
		
		if(value.compareTo(min) < 0)
			minAt = time;
		min = min.min(value);

		if(value.compareTo(max) > 0)
			maxAt = time;
		max = max.max(value);
	}

	private void initValues(long time, BigDecimal value) {
		min = value;
		max = value;
		minAt = time;
		maxAt = time;
		valueAtBeginWindow = null;
		if(startOfTheWindow == time) //we only do valueAtBeginOfWindow if the time is the first time at start of window
			valueAtBeginWindow = value;
	}

	@Override
	protected TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow) {
		TSRelational r = new TSRelational(timeColumn, valueColumn);
		r.setTime(startOfWindow);
		if(min != null) {
			r.put("min", min+"");
			r.put("minAt", minAt+"");
			r.put("max", max+"");
			r.put("maxAt", maxAt+"");
			if(valueAtBeginWindow != null)
				r.put("value", valueAtBeginWindow+"");
		}
		
		min = null;
		max = null;
		minAt = null;
		maxAt = null;
		
		return r;
	}

}
