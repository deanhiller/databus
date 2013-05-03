package controllers.modules;

import gov.nrel.util.TimeValue;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import controllers.modules.util.JsonRow;

public class SplineCalculations {

	private long interval;
	private long currentTimePointer;
	private CircularFifoBuffer buffer;
	private long end;
	private boolean isInitialized;

	public SplineCalculations(CircularFifoBuffer buffer, long start, long end, long interval) {
		this.buffer = buffer;
		this.interval = interval;
		currentTimePointer = start;
		this.end = end;
	}

	public void processSingleChunk(List<TimeValue> rows, List<TimeValue> interpolatedTimes) {
		initialize(rows);
		
		//keep processing until rowCount is below the window size(ie. we can no longer interpolate)
		while(rows.size() > 0) {
			transferRows(rows, buffer);
			calculate(interpolatedTimes);
		}
	}

	private void initialize(List<TimeValue> rows) {
		if(isInitialized)
			return;
		
		TimeValue first = rows.get(0);
		TimeValue veryFirst = new TimeValue(currentTimePointer, first.getValue());
		buffer.add(veryFirst);
		
		isInitialized = true;
	}

	private static void transferRows(List<TimeValue> rows, CircularFifoBuffer buffer) {
		int windowSize = buffer.maxSize();
		if(buffer.size() > 0)
			windowSize--; //we have enough in the buffer, make it overlap

		//we may not have windowSize datapoints so copy the rows left..
		int rowsNeeded = Math.min(windowSize, rows.size());

		int counter = 0;
		while(counter < rowsNeeded && rows.size() > 0) {
			TimeValue row = rows.remove(0);
			if(row.getValue() != null) {
				buffer.add(row);
				counter++;
			}
		}
	}
	
	private void calculate(List<TimeValue> interpolatedTimes) {
		Spline s = createSpline();
		Object[] objects = buffer.toArray();
		TimeValue lastVal = (TimeValue) objects[objects.length-1];

		TimeValue firstInBuf = (TimeValue) objects[0];
		if (currentTimePointer < firstInBuf.getTime()) {
			interpolatedTimes.add(new TimeValue(currentTimePointer, firstInBuf.getValue()));
			currentTimePointer+=interval;
		}
		
		while(currentTimePointer <= lastVal.getTime() && currentTimePointer <= end) {
			double value = s.spline_value(currentTimePointer);
			//TODO:JSC  there is a potential bug here.  value can be NaN which is not supported by BigDecimal.
			//What to do in this case?  Use this to duplicate the problem:
			//http://localhost:9000/api/splineV1/60000/4/rangecleanV1/-2000000/2000000/rawdataV1/fakeTimeSeries/9651/99999999
			if (Double.isNaN(value) || Double.isInfinite(value))
				interpolatedTimes.add(new TimeValue(currentTimePointer, null));
			else
				interpolatedTimes.add(new TimeValue(currentTimePointer, BigDecimal.valueOf(value)));
			currentTimePointer += interval;
		}
	}

	private Spline createSpline() {
		if(buffer.size() < buffer.maxSize()) {
			//we have to have 4 in the buffer to spline
			fillTheBuffer();
		}
		
		Object[] array = buffer.toArray();
		double[] times = new double[buffer.maxSize()];
		double[] values = new double[buffer.maxSize()];
		for(int i = 0; i < buffer.maxSize(); i++) {
			TimeValue tv = (TimeValue)array[i];
			times[i] = tv.getTime();
			values[i] = tv.getValue()==null?null:tv.getValue().doubleValue();
		}
		Spline s = new Spline(times, values);
		return s;
	}

	private void fillTheBuffer() {
		Object[] array = buffer.toArray();
		TimeValue last = (TimeValue) array[array.length-1];

		int counter = buffer.size();
		for(int i = 1; counter < buffer.maxSize(); i++, counter++) {
			long newTime = last.getTime()+(i*interval);
			buffer.add(new TimeValue(newTime, last.getValue()));
		}
	}

	public void processLeftOver(List<TimeValue> interpolatedTimes) {
		if (buffer.size() != 0) {
			TimeValue[] array = (TimeValue[]) buffer.toArray(new TimeValue[]{});
	
			while(currentTimePointer <= end ) {
				TimeValue copiedVal = new TimeValue(currentTimePointer, array[array.length-1].getValue());
				interpolatedTimes.add(copiedVal);
				currentTimePointer = currentTimePointer + interval;
			}
		}
	}
	
}
