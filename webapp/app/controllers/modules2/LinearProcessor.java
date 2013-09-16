package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gov.nrel.util.TimeValue;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;

import play.mvc.results.BadRequest;
import controllers.modules.SplinesBigDec;
import controllers.modules.SplinesBigDecBasic;
import controllers.modules.SplinesBigDecLimitDerivative;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class LinearProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);

	private CircularFifoBuffer buffer = new CircularFifoBuffer(2);
	private SplinesLinear linear = new SplinesLinear();
	private long epochOffset;
	protected long interval;
	private long currentTimePointer;
	private ReadResult lastValue;
	private long end;

	private boolean isSplineCreated;

	@Override
	protected int getNumParams() {
		return 0;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);

		// param 2: Interval: long
		String intervalStr = fetchProperty("interval", "60000", options);
		try {
			interval = Long.parseLong(intervalStr);
			if (interval < 1) {
				String msg = "/linearV1(interval="+interval+")/ ; interval must be > 0 ";
				throw new BadRequest(msg);
			}
		} catch (NumberFormatException e) {
			String msg = "/linearV1(interval="+intervalStr+")/ ; interval is not a long ";
			throw new BadRequest(msg);
		}

		String epoch = options.get("epochOffset");
		if(epoch == null) {
			epochOffset = calculateOffset();
		} else
			epochOffset = parseOffset(epoch);

		long startTime = Long.MIN_VALUE;
		if(params.getStart() != null)
			startTime = params.getStart();
		end = Long.MAX_VALUE;
		if(params.getEnd() != null)
			end = params.getEnd();

		if(log.isInfoEnabled())
			log.info("offset="+epochOffset+" start="+startTime+" interval="+interval);
		currentTimePointer = calculateStartTime(startTime, interval, epochOffset);

		return newPath;
	}

	protected long calculateOffset() {
		Long startTime = params.getStart();
		if(startTime != null) {
			long offset = startTime % interval;
			return offset;
		}
		return 0;
	}

	protected long parseOffset(String offset) {
		try {
			return Long.parseLong(offset);
		} catch (NumberFormatException e) {
			String msg = "/linearV1(epochOffset="+offset+")/ epochOffset is not a long";
			throw new BadRequest(msg);
		}
	}

	private String fetchProperty(String key, String defaultVal, HashMap<String, String> options) {
		String s = options.get(key);
		if(s != null)
			return s;
		return defaultVal;
	}

	public static long calculateStartTime(long startTime, long interval, Long epochOffset) {
		if(epochOffset == null)
			return startTime;

		long rangeFromOffsetToStart = startTime-epochOffset;
		long offsetFromStart = -rangeFromOffsetToStart % interval;
		if(startTime > 0) {
			offsetFromStart = interval - (rangeFromOffsetToStart%interval);
		}

		long result = startTime+offsetFromStart; 
		if(startTime == Long.MIN_VALUE) {
			result = interval+offsetFromStart+startTime;
		}
		if(log.isInfoEnabled())
			log.info("range="+rangeFromOffsetToStart+" offsetFromStart="+offsetFromStart+" startTime="+startTime+" result="+result);
		return result;
	}

	@Override
	public ReadResult read() {
		//we are ready as soon as one of the streams has enough data in the buffer(ie. 4 points needs for a spline)
		//the other streams will have to return null until they have enough data points is all.
		while(!anyStreamIsReady()) {
			pull();
			if (lastValue == null) {
				return null;
			} else if (lastValue.isMissingData()) {
				return lastValue;
			} else if (lastValue.isEndOfStream()) {
				return lastValue;
			} else {
				transferRow(lastValue.getRow());
			}
		}

		//FIRST is move currentTimePointer so currentTime > 1st point time
		while(currentTimeLessThan1stPtAndBufferFull(currentTimePointer)) {
			incrementCurrentTime();
		}

		while(needMoreData() && currentTimePointer <= end) {
			pull();
			if (lastValue == null) {
				return null;
			} else if (lastValue.isMissingData()) {
				return lastValue;
			} else if (lastValue.isEndOfStream()) {
				return lastValue;
			} else {
				transferRow(lastValue.getRow());
				isSplineCreated = false;
			}
		}

		//NOW, we are in a state where AT LEAST one column has 2nd timept < currentTime < 3rd timept so we can spline
		//for that stream(hopefully this is true for a few of the columns)
		ReadResult returnVal = null;
		if (currentTimePointer <= end) {
			returnVal = calculate();
		} else {
			//signify end of stream
			returnVal = new ReadResult();
		}

		return returnVal;
	}

	private boolean needMoreData() {
		long time = fetchTime(1);
		if(currentTimePointer > time)
			return true;
		return false;
	}

	private boolean currentTimeLessThan1stPtAndBufferFull(long currentTimePointer2) {
		long time = fetchTime(0);
		if(currentTimePointer2 < time)
			return true;
		return false;
	}

	private long fetchTime(int index) {
		TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
		TSRelational secondInBuf = objects[index];
		long timePt = ((BigInteger)secondInBuf.get(timeColumn)).longValue();
		return timePt;
	}

	private boolean anyStreamIsReady() {
		if(buffer.isFull())
			return true;
		return false;
	}

	private void transferRow(TSRelational row) {
		buffer.add(row);
	}

	private void pull() {
		PullProcessor ch = getChild();
		lastValue = ch.read();
	}

	private ReadResult calculate() {
		TSRelational row = new TSRelational();

		if(!isSplineCreated) {
			createSpline();
			isSplineCreated = true;
		}

		BigDecimal val = linear.getValue(currentTimePointer);
		row.put(timeColumn, new BigInteger(""+currentTimePointer));
		row.put(valueColumn, val);
		log.debug("-------- returning a point t(for col="+valueColumn+")="+row.getTime()+" buf="+buffer);

		incrementCurrentTime();
		
		return new ReadResult(getUrl(), row);
	}

	private void createSpline() {
		TSRelational[] array = (TSRelational[])buffer.toArray(new TSRelational[]{});
		long[] times = new long[buffer.maxSize()];
		BigDecimal[] values = new BigDecimal[buffer.maxSize()];
		for (int i = 0; i < buffer.maxSize(); i++) {
			TSRelational tv = (TSRelational) array[i];
			times[i] = tv.getTime();
			values[i] = (BigDecimal) tv.get(valueColumn);
		}
		linear.setRawDataPoints(times, values);
	}

	private void incrementCurrentTime() {
		currentTimePointer += interval;
	}

	private enum UninterpalatedValueMethod {
		PREVIOUS_ROW,NEAREST_ROW
	}

}
