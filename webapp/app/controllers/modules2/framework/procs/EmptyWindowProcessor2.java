package controllers.modules2.framework.procs;

import java.math.BigDecimal;
import java.math.BigInteger;

import controllers.modules2.EmptyFlag;
import controllers.modules2.SplinesPullProcessor;
import controllers.modules2.SplinesV3PullProcessor;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;

/**
 * This abstract class is for those processors that give back a data point PER window and this class takes care of the case where a window
 * has no data points in it by simply calling incomingDataPoint over and over and then calling fetchLastWindow so the subclass can concentrate
 * on just taking all the data that was shoved into it and passing one data point back(or if there was no data in that window, just returning
 * something like 0 or null or whatever he decides is appropriate.....we could modify this so returning null to EmptyWindowProcessor means to 
 * skip returning a value in both push and pull cases but for now we assume a value is always returned).
 * 
 * @author dhiller2
 *
 */
public abstract class EmptyWindowProcessor2 extends PullProcessorAbstract {

	protected long startOfTheWindow;
	private long endOfTheWindow;
	private long end;
	private long interval;
	private ReadResult lastRead;
	private boolean hasData;

	public void initEmptyParser(long start, long end2, long interval2, Long epochOffset) {
		long half = interval2 / 2;
		startOfTheWindow = SplinesV3PullProcessor.calculateStartTime(start, interval2, epochOffset) + half;
		this.end = end2;
		interval = interval2;
		initEndOfWindow();
	}

	private void initEndOfWindow() {
		endOfTheWindow = Math.min(startOfTheWindow+interval, end);
	}

	@Override
	public ReadResult read() {
		PullProcessor proc = (PullProcessor) getSingleChild();
		
		//1. read a row in
		//2. check if need to write out previous window and return
		//3. OR if not need, then check if need to fast forward to row's time(so row is in the window)
		while(startOfTheWindow < end) {
			ReadResult read = readNextValue(proc);
			if(read.isEndOfStream()) {
				if(!hasData)
					return read;
				ReadResult res = fetchWindowResult();
				hasData = false;
				return res;
			} else if(read.isMissingData())
				return read;

			TSRelational row = read.getRow();
			long time = getTime(row);
			
			if(time > endOfTheWindow) {
				if(hasData) {
					//we are past the end of a window and had data so we need to return a row for
					//this window...	
					ReadResult res = fetchWindowResult();
					//cache the value for next read
					this.lastRead = read;
					hasData = false;
					return res;
				} else {
					BigInteger time1 = new BigInteger(time+"");
					BigInteger time2 = new BigInteger(startOfTheWindow+"");
					BigInteger diff = time1.subtract(time2);
					BigInteger multiplier = diff.divideAndRemainder(new BigInteger(interval+""))[0];
					long multiply = multiplier.longValue();
					long add = multiply*interval;
					startOfTheWindow += add;
					initEndOfWindow();
				}
			}
	
			hasData = true;
			incomingTimeValue(time, row);
		}
		
		return new ReadResult();
	}

	private ReadResult readNextValue(PullProcessor proc) {
		if(lastRead != null) {
			ReadResult res = lastRead;
			lastRead = null; //clear the cache
			//return cached value
			return res;
		}
		
		ReadResult read = proc.read();
		return read;
	}

	private ReadResult fetchWindowResult() {
		TSRelational windowsRow = readDataForWindow();
		return new ReadResult(null, windowsRow);
	}
	
	private TSRelational readDataForWindow() {
		TSRelational r = readLastWindowsValue(startOfTheWindow, endOfTheWindow, interval);
		
		startOfTheWindow += interval;
		initEndOfWindow();
		return r;
	}

	protected abstract void incomingTimeValue(long time, TSRelational value);
	protected abstract TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow, long interval);

	//unused
	protected TSRelational modifyRow(TSRelational row) {
		throw new UnsupportedOperationException("not supported ever for window processors");
	}
	
	public void setChild(PullProcessor mock) {
		children.add(mock);
	}
}
