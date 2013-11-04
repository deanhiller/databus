package controllers.modules2.framework.procs;

import java.math.BigDecimal;

import controllers.modules2.EmptyFlag;
import controllers.modules2.SplinesPullProcessor;
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
public abstract class EmptyWindowProcessor extends PushOrPullProcessor {

	protected long startOfTheWindow;
	private long endOfTheWindow;
	private long end;
	private long interval;
	private ReadResult lastRead;

	public void initEmptyParser(long start, long end2, long interval2, Long epochOffset) {
		startOfTheWindow = SplinesPullProcessor.calculateStartTime(start, interval2, epochOffset);
		this.end = end2;
		interval = interval2;
		initEndOfWindow();
	}

	private void initEndOfWindow() {
		endOfTheWindow = Math.min(startOfTheWindow+interval, end);
	}

	@Override
	public final void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		long time = getTime(row);

		while(endOfTheWindow < time) {
			sendWindow(url, flag);
		}
		
		incomingTimeValue(time, row);
	}

	private void sendWindow(String url, ProcessedFlag flag) {
		TSRelational r = readDataForWindow();
		getNextInChain().incomingChunk(url, r, flag);
	}

	public void complete(String url) {
		EmptyFlag flag = new EmptyFlag();
		while(endOfTheWindow < end) {
			sendWindow(url, flag);
		}
		//send the very last window
		sendWindow(url, flag);
		super.complete(url);
	}

	@Override
	public ReadResult read() {
		PullProcessor proc = (PullProcessor) getSingleChild();
		
		while(startOfTheWindow < end) {
			ReadResult read = readNextValue(proc);
			if(read.isEndOfStream()) {
				return fetchWindowResult();
			} else if(read.isMissingData())
				return read;

			TSRelational row = read.getRow();
			long time = getTime(row);
			
			if(endOfTheWindow < time) {
				ReadResult res = fetchWindowResult();
				//cache the value for next read
				this.lastRead = read;
				return res;
			}
	
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
		TSRelational r = readLastWindowsValue(startOfTheWindow, endOfTheWindow);
		
		startOfTheWindow += interval;
		initEndOfWindow();
		return r;
	}

	protected abstract void incomingTimeValue(long time, TSRelational value);
	protected abstract TSRelational readLastWindowsValue(long startOfWindow, long endOfWindow);

	//unused
	protected TSRelational modifyRow(TSRelational row) {
		throw new UnsupportedOperationException("not supported ever for window processors");
	}
	
	public void setChild(PullProcessor mock) {
		children.add(mock);
	}
}
