package controllers.modules;

import gov.nrel.util.TimeValue;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.mortbay.log.Log;

import play.Logger;

import controllers.modules.util.JsonRow;

public class SplinesCalculations {

	private long interval;
	private long currentTimePointer;
	private CircularFifoBuffer buffer;
	private long end;
	private boolean isInitialized;
	private Splines splines;
	private long maxGeneratedPoints;
	private boolean consumedPoint = true;
	private long direction;

	public SplinesCalculations(CircularFifoBuffer buffer, long start, long end, long interval, Splines splines, long maxGeneratedPoints) {
		this.buffer = buffer;
		this.interval = interval;
		currentTimePointer = start;
		this.end = end;
		this.splines = splines;
		this.maxGeneratedPoints = maxGeneratedPoints;
		this.direction = Math.round(Math.signum(end-start));
	}

	public boolean processSingleChunk(List<TimeValue> rows, List<TimeValue> interpolatedTimes) {
		initialize(rows);
		
		//keep processing until rowCount is below the window size(ie. we can no longer interpolate)
		
		// maybe should have (to prevent the fill method from being called???
		
		while(rows.size() > 0) {
			if (consumedPoint)
				transferRows(rows, buffer);
			if(buffer.size() == buffer.maxSize()) {
				calculate(interpolatedTimes);
				return false;
			}
		}
		return true;
	}

	private void initialize(List<TimeValue> rows) {
		if(isInitialized)
			return;
		
		TimeValue first = rows.get(0);
		TimeValue veryFirst = new TimeValue(currentTimePointer, first.getValue());
		buffer.add(veryFirst);
		// double up the first point if the first point in rows is not the same as the currentTimePointer
		if(currentTimePointer != first.getTime()){
			buffer.add(veryFirst);
		}
		
		isInitialized = true;
	}

	private static void transferRows(List<TimeValue> rows, CircularFifoBuffer buffer) {
		int windowSize = buffer.maxSize();
		//if(buffer.size() > 0)
			//windowSize--; //we have enough in the buffer, make it overlap
		
		// Fill empty positions, or at most shift the buffer by 1
		windowSize = buffer.maxSize()-buffer.size();
		if(windowSize == 0) windowSize = 1;

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
	
	private boolean calculate(List<TimeValue> interpolatedTimes) {
		createSpline();
		Object[] objects = buffer.toArray();
		TimeValue secondToLastVal;
		secondToLastVal = (TimeValue) objects[objects.length-2];
//		if(direction > 0){
//			secondToLastVal = (TimeValue) objects[objects.length-2];
//		} else {
//			secondToLastVal = (TimeValue) objects[1];
//		}
		interpolatedTimes.clear();

		// This part should never run after the initialize
		// we really want second in buffer ...
		//TimeValue firstInBuf = (TimeValue) objects[0];
		TimeValue secondInBuf = (TimeValue) objects[1];
		if( (currentTimePointer*direction) < (secondInBuf.getTime()*direction) )
			throw new RuntimeException("The currentTimePointer should never be before the second point in the Buffer (this is our bug) ctp="+currentTimePointer+" second point ="+secondInBuf.getTime()); 
		
		int numGeneratedPoints = 0;
		while( (currentTimePointer*direction) <= (secondToLastVal.getTime()*direction) && (currentTimePointer*direction) <= (end*direction) ) {
			if (numGeneratedPoints < maxGeneratedPoints) {
				consumedPoint = false;
				numGeneratedPoints++;
				double value = splines.getValue(currentTimePointer);
				interpolatedTimes.add(new TimeValue(currentTimePointer, BigDecimal.valueOf(value)));
				currentTimePointer += interval*direction;
			}
			else
				return false;
		}
		consumedPoint = true;
		return true;
	}

	private void createSpline() {
		
		Object[] array = buffer.toArray();
		double[] times = new double[buffer.maxSize()];
		double[] values = new double[buffer.maxSize()];
		for(int i = 0; i < buffer.maxSize(); i++) {
			TimeValue tv = (TimeValue)array[i];
			times[i] = tv.getTime();
			values[i] = tv.getValue()==null?0.0d:tv.getValue().doubleValue();
		}
		//Splines s = new SplinesLimitDerivative(times, values);
		//Splines s = new SplinesBasic(times, values);
		splines.setRawDataPoints(times, values);
//		return s;
	}


	public boolean processLeftOver(List<TimeValue> interpolatedTimes) {
		
		if (buffer.size() != 0) {
			TimeValue[] array = (TimeValue[]) buffer.toArray(new TimeValue[]{});
	
			while( (currentTimePointer*direction) <= (end*direction) ) {
				//TimeValue copiedVal = new TimeValue(currentTimePointer, array[array.length-1].getValue());
				//interpolatedTimes.add(copiedVal);
				//currentTimePointer = currentTimePointer + interval;
				if (consumedPoint) {
					TimeValue copiedVal = new TimeValue(end, array[array.length-1].getValue());
					buffer.add(copiedVal);
				}
				return calculate(interpolatedTimes) && (currentTimePointer*direction) > (end*direction);
				//after two passes it should be done (as long as times are long this will not be a problem)
			}
		}
		return true;
	}
	
}
