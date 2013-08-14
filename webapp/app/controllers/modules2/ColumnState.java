package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.SplinesBigDec;
import controllers.modules.SplinesBigDecBasic;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;

public class ColumnState {
	private static final Logger log = LoggerFactory.getLogger(ColumnState.class);

	private CircularFifoBuffer buffer = new CircularFifoBuffer(4);
	private List<TSRelational> leftOver = new ArrayList<TSRelational>();
	private SplinesBigDec spline;
	private boolean isSplineCreated;
	private String timeCol;
	private String colName;
	private long maxToStopSplining;
	private int bufferSize;

	public ColumnState(SplinesBigDec splinesBigDecBasic, String timeCol, String colName, int bufferSize, long maxTimeToStopSplining) {
		spline = splinesBigDecBasic;
		this.timeCol = timeCol;
		this.colName = colName;
		this.maxToStopSplining = maxTimeToStopSplining;
		this.bufferSize = bufferSize;
	}

	public CircularFifoBuffer getBuffer() {
		return buffer;
	}

	public void transferRow(TSRelational row) {
		Object value = row.get(colName);
		if(value == null)
			return; //we don't want this row in this column state as null does not help us spline ;).

		if(buffer.isFull()) {
			//If leftOver is not FULL(ie. equal to bufferSize, we can just add to leftOver)...
			boolean wasFull = isLeftOverBufferFull();
			leftOver.add(row); //this may be buffer full +1 more!!!!!
			if(wasFull) {
				//reduce the buffer size to being full(or less)
				TSRelational txfrRow = leftOver.remove(0);
				buffer.add(txfrRow);
				isSplineCreated = false;
			}
			return;
		}

		buffer.add(row);
		isSplineCreated = false;
	}

	public void calculate(TSRelational row, long currentTimePointer) {
		long timeSecondInBuffer = fetchSecondTime();
		long timeThirdInBuffer = fetchThirdTime();
		long difference = timeThirdInBuffer - timeSecondInBuffer;
		
		if(currentTimePointer > timeThirdInBuffer || currentTimePointer < timeSecondInBuffer) {
			//this is a special case where either
			//1. our buffers have maxed out and we don't have enough data to spline this data point OR
			//2. we are at the end of all the rows but have not reached the "end" specified by the user
			row.put(colName, null);
			return;
		}
		
		if(difference >= maxToStopSplining) {
			//There is too much space between 2nd and 3rd point which means the client does not want values returns as
			//he would like to see the large gaps(we only spline for smaller gaps)
			row.put(colName, null);
			return;
		} else if(!isSplineCreated) {
			createSpline();
			isSplineCreated = true;
		}

		BigDecimal val = spline.getValue(currentTimePointer);
		row.put(colName, val);
		log.debug("-------- returning a point t(for col="+colName+")="+row.getTime()+" other points are timeSecond="+timeSecondInBuffer+" timeThird="+timeThirdInBuffer);
	}

	private void createSpline() {
		TSRelational[] array = (TSRelational[])buffer.toArray(new TSRelational[]{});
		long[] times = new long[buffer.maxSize()];
		BigDecimal[] values = new BigDecimal[buffer.maxSize()];
		for (int i = 0; i < buffer.maxSize(); i++) {
			TSRelational tv = (TSRelational) array[i];
			times[i] = tv.getTime();
			values[i] = (BigDecimal) tv.get(colName);
		}
		spline.setRawDataPoints(times, values);
	}
	
	public void prepareBuffer(long currentTimePointer) {
		if(!buffer.isFull())
			return; //only prepare buffer if it is full of values.  If not, we can't do much yet

		long thirdTime = fetchThirdTime();
		while(currentTimePointer > thirdTime && leftOver.size() > 0) {
			TSRelational row = leftOver.remove(0);
			buffer.add(row);
			thirdTime = fetchThirdTime();
			isSplineCreated = false;
		}
	}

	public boolean needMoreData(long currentTimePointer) {
		//We need more data if our buffer is not full
		if(!buffer.isFull())
			return true;
		
		long third = fetchThirdTime();
		// we need more data is currentTimePointer has passed the middle two spline points
		if(currentTimePointer > third) 
			return true;
		return false;
	}

	private long fetchThirdTime() {
		return fetchTime(2);
	}

	private long fetchSecondTime() {
		return fetchTime(1);
	}

	private long fetchTime(int index) {
		TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
		TSRelational secondInBuf = objects[index];
		long timeSecondInBuffer = ((BigInteger)secondInBuf.get(timeCol)).longValue();
		return timeSecondInBuffer;
	}

//	private void recalc2nd3rdTimes() {
//	
//
//	TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
//	TSRelational secondInBuf = objects[1];
//	TSRelational secondToLastVal = objects[objects.length - 2];
//
//	timeSecondInBuffer = ((BigInteger)secondInBuf.get(timeCol)).longValue();
//	timeThirdInBuffer = ((BigInteger)secondToLastVal.get(timeCol)).longValue();
//}
	public String getColumn() {
		return colName;
	}

	public boolean secondPointGreaterThan(long currentTime) {
		long second = fetchSecondTime();
		if(second > currentTime)
			return true;
		return false;
	}

	public boolean isLeftOverBufferFull() {
		if(leftOver.size() >= bufferSize)
			return true;
		return false;
	}

}
