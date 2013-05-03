package controllers.modules;

import gov.nrel.util.TimeValue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import controllers.modules.util.JsonRow;

public class AggCache {

	private List<TimeValue> alignedTimes = new ArrayList<TimeValue>();
	private List<TimeValue> unalignedTimes = new ArrayList<TimeValue>();
	private boolean complete;
	private static final int WINDOW = 5;
	private SplineCalculations splines;
	private String url;
	
	public AggCache(String url) {
		this.url = url;
	}
	
	public void addChunk(JsonRow chunk, boolean initialized) {
		unalignedTimes.addAll(chunk.getRows());
	}

	public int getRowCount() {
		return unalignedTimes.size();
	}

	public long getAverageInterval() {
		List<TimeValue> rows = getRows(10);
		List<Long> intervals = new ArrayList<Long>();

		long previousTime = rows.get(0).getTime();
		for(int i = 1; i < rows.size(); i++) {
			long time = rows.get(i).getTime();
			intervals.add(time-previousTime);
			previousTime = time;
		}

		long total = 0;
		for(Long time : intervals) {
			total += time;
		}

		return total/intervals.size();
	}

	private List<TimeValue> getRows(int count) {
		List<TimeValue> rows = new ArrayList<TimeValue>();
		for(TimeValue tv : unalignedTimes) {
			rows.add(tv);
			if(rows.size() >= count)
				return rows;
		}

		throw new RuntimeException("bug, we should not reach here");
	}
	
	public void initialize(long start, long end, long interval) {
		CircularFifoBuffer buffer = new CircularFifoBuffer(WINDOW);
		splines = new SplineCalculations(buffer, start, end, interval);
		
		splines.processSingleChunk(unalignedTimes, alignedTimes);
	}

	public void setComplete(boolean b) {
		this.complete = b;
	}

	public Integer getAlignedRowCount2() {
		if(complete)
			return null;
		return alignedTimes.size();
	}

	public TimeValue removeNextAlignedRow() {
		if(alignedTimes.size() == 0)
			return null; //list is exhausted so return null
		return alignedTimes.remove(0);
	}

	public void processLeftOver() {
		//in some cases, there was only one, two, or three datapoints
		if(splines != null)
			splines.processLeftOver(alignedTimes);
	}

	public String getUrl() {
		return url;
	}
}
