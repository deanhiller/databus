package controllers.modules2;

import java.math.BigDecimal;

import gov.nrel.util.TimeValue;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

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

public class SplinesPullProcessor extends PullProcessorAbstract {

	private String splineType;
	private long interval;
	private SplinesBigDec spline;
	private boolean isSplineCreated = false;

	long timeSecondInBuffer = 0;
	long timeThirdInBuffer = 0;

	private CircularFifoBuffer buffer;

	private long currentTimePointer;

	private ReadResult lastValue;
	private long epochOffset;

	@Override
	protected int getNumParams() {
		return 3;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor) {
		String newPath = super.init(path, nextInChain, visitor);
		// param 1: Type: String
		splineType = params.getParams().get(0);
		/**
		 * Current spline options: basic -> SplinesBigDecBasic limitderivative
		 * -> SplinesBigDecLimitDerivative
		 */
		if ("basic".equals(splineType)) {
			spline = new SplinesBigDecBasic();
		} else if ("limitderivative".equals(splineType)) {
			spline = new SplinesBigDecLimitDerivative();
		} else {
			// fix this bad request line
			String msg = "/splinesV1BetaBigDec/{type}/{interval}/{epochOffset} ; type must be basic or limitderivative";
			throw new BadRequest(msg);
		}

		// param 2: Interval: long
		try {
			interval = Long.parseLong(params.getParams().get(1));
			if (interval < 1) {
				String msg = "/splinesV1BetaBigDec/{type}/{interval}/{epochOffset} ; interval must be > 0 ";
				throw new BadRequest(msg);
			}
		} catch (NumberFormatException e) {
			String msg = "/splinesV1BetaBigDec/{type}/{interval}/{epochOffset} ; interval is not a long ";
			throw new BadRequest(msg);
		}

		try {
			epochOffset = Long.parseLong(params.getParams().get(2));
		} catch (NumberFormatException e) {
			String msg = "/splinesV1BetaBigDec/{type}/{interval}/{epochOffset} ; epochOffset is not a long";
			throw new BadRequest(msg);
		}

		if(params.getStart() == null || params.getEnd() == null) {
			String msg = "splinesV1BetaBigDec must have a start and end (if you want it to work, request it)";
			throw new BadRequest(msg);
		}
		Long startTime = params.getStart();
		currentTimePointer = calculateStartTime(startTime, interval, epochOffset);
		
		// setup buffer
		this.buffer = new CircularFifoBuffer(4);
		return newPath;
	}

	public static long calculateStartTime(long startTime, long interval, Long epochOffset) {
		if(epochOffset == null)
			return startTime;
		
		long rangeFromOffsetToStart = startTime-epochOffset;
		long offsetFromStart = rangeFromOffsetToStart % interval;
		if(startTime > 0) {
			offsetFromStart = interval - (rangeFromOffsetToStart%interval);
		}
		return startTime-offsetFromStart;
	}

	@Override
	public ReadResult read() {
		// fill the buffer (stop if reach end of stream)
		while ((buffer.size() != buffer.maxSize())) {
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

		if(currentTimePointer < timeSecondInBuffer) {
			//move currentTimePointer PAST the timeSecondInBuffer
			long range = (timeSecondInBuffer)-currentTimePointer;
			long multiplier = range / interval;
			currentTimePointer = currentTimePointer + (interval*multiplier);
			if(currentTimePointer < timeSecondInBuffer) {
				//add one more interval for the case where currentTimePointer != timeSecondInBuffer, otherwise, the times now match up exactly
				currentTimePointer += interval;
			}
		}

		//NOW, we may have moved past the third time in the buffer so may need to read in more data
		long end = params.getEnd();
		while ((currentTimePointer > timeThirdInBuffer)
				&& (currentTimePointer <= end)) {
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

		// make sure that we didn't add too much (only error checking)
		if (currentTimePointer < timeSecondInBuffer)
			throw new RuntimeException(
					"The currentTimePointer should never be before the second point in the Buffer (this is our bug) ctp="
							+ currentTimePointer
							+ " second point ="
							+ timeSecondInBuffer);

		ReadResult returnVal = null;
		if (currentTimePointer <= end) {
			returnVal = calculate();
		} else {
			returnVal = new ReadResult();
		}

		return returnVal;
	}

	private void pull() {
		PullProcessor ch = getChild();
		lastValue = ch.read();
	}

	private void transferRow(TSRelational row) {
		buffer.add(toTimeValue(row));
		if (buffer.size() == buffer.maxSize()) {
			isSplineCreated = false;

			Object[] objects = buffer.toArray();
			TimeValue secondToLastVal;
			secondToLastVal = (TimeValue) objects[objects.length - 2];
			TimeValue secondInBuf = (TimeValue) objects[1];

			timeSecondInBuffer = secondInBuf.getTime();
			timeThirdInBuffer = secondToLastVal.getTime();
		}
	}

	private ReadResult calculate() {
		if(!isSplineCreated){
			createSpline();
			isSplineCreated = true;
		}
		ReadResult out;
		BigDecimal value = spline.getValue(currentTimePointer);
		out = pointOut(currentTimePointer, value);
		currentTimePointer += interval;
		return out;
	}

	private ReadResult pointOut(long time, BigDecimal value) {
		TSRelational rowout = new TSRelational();
		setTime(rowout, time);
		setValue(rowout, value);
		return new ReadResult(getUrl(), rowout);
	}

	// all code past here should be exactly the same as the SplinesProcessor
	// code
	private void initialize(TSRelational row) {
		TimeValue first = toTimeValue(row);
		TimeValue veryFirst = new TimeValue(currentTimePointer,
				first.getValue());
		buffer.add(veryFirst);
		// double up the first point if the first point in rows is not the same
		// as the currentTimePointer
		if (currentTimePointer != first.getTime()) {
			buffer.add(veryFirst);
		}
	}

	private TimeValue toTimeValue(TSRelational row) {
		TimeValue rowTimeValue = new TimeValue(getTime(row), getValue(row));
		return (rowTimeValue);
	}

	private void createSpline() {
		Object[] array = buffer.toArray();
		long[] times = new long[buffer.maxSize()];
		BigDecimal[] values = new BigDecimal[buffer.maxSize()];
		for (int i = 0; i < buffer.maxSize(); i++) {
			TimeValue tv = (TimeValue) array[i];
			times[i] = tv.getTime();
			values[i] = tv.getValue() == null ? BigDecimal.valueOf(0.0) : tv
					.getValue();
		}
		spline.setRawDataPoints(times, values);
	}

	public void setChild(ProcessorSetup mock) {
		this.child = mock;
	}

	// TSRelational row = new TSRelational();
	// setValue(row, val);
	// setTime(row, val);
	// new ReadResult(getUrl(),row);
}
