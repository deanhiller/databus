package controllers.modules2;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nrel.util.TimeValue;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);
	private String splineType;
	private List<String> columnsToInterpolate;
	protected long interval;
	private Map<String, SplinesBigDec> spline = new HashMap<String, SplinesBigDec>();
	private boolean isSplineCreated = false;

	long timeSecondInBuffer = 0;
	long timeThirdInBuffer = 0;
	
	private UninterpalatedValueMethod uninterpalatedValueMethod = UninterpalatedValueMethod.NEAREST_ROW;

	private CircularFifoBuffer buffer;
	//private List<TSRelational> buffer;
	
	private long currentTimePointer;

	private ReadResult lastValue;
	private long epochOffset;

	@Override
	protected int getNumParams() {
		return 3;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);
		columnsToInterpolate=Arrays.asList(new String[]{valueColumn});
		String columnsToInterpolateString = options.get("columnsToInterpolate");
		if (StringUtils.isNotBlank(columnsToInterpolateString)) {
			columnsToInterpolate = Arrays.asList(StringUtils.split(columnsToInterpolateString, ";"));
		}
		String uninterpalatedValueMethodString = options.get("uninterpalatedValueMethod");
		if (StringUtils.equalsIgnoreCase("previous", uninterpalatedValueMethodString))
			uninterpalatedValueMethod = UninterpalatedValueMethod.PREVIOUS_ROW;
			
		// param 1: Type: String
		splineType = params.getParams().get(0);
		/**
		 * Current spline options: basic -> SplinesBigDecBasic limitderivative
		 * -> SplinesBigDecLimitDerivative
		 */
		if ("basic".equals(splineType)) {
			for (String colname:columnsToInterpolate)
				spline.put(colname, new SplinesBigDecBasic());
		} else if ("limitderivative".equals(splineType)) {
			for (String colname:columnsToInterpolate)
				spline.put(colname, new SplinesBigDecLimitDerivative());
		} else {
			// fix this bad request line
			String msg = "/splinesV2/{type}/{interval}/{epochOffset} ; type must be basic or limitderivative";
			throw new BadRequest(msg);
		}

		// param 2: Interval: long
		try {
			interval = Long.parseLong(params.getParams().get(1));
			if (interval < 1) {
				String msg = "/splinesV2/{type}/{interval}/{epochOffset} ; interval must be > 0 ";
				throw new BadRequest(msg);
			}
		} catch (NumberFormatException e) {
			String msg = "/splinesV2/{type}/{interval}/{epochOffset} ; interval is not a long ";
			throw new BadRequest(msg);
		}

		epochOffset = calculateOffset();

		if(params.getStart() == null || params.getEnd() == null) {
			String msg = "splinesV2 must have a start and end (if you want it to work, request it)";
			throw new BadRequest(msg);
		}
		
		Long startTime = params.getStart();
		if(log.isInfoEnabled())
			log.info("offset="+epochOffset+" start="+startTime+" interval="+interval);
		currentTimePointer = calculateStartTime(startTime, interval, epochOffset);
		
		// setup buffer
		this.buffer = new CircularFifoBuffer(4);
		return newPath;
	}

	protected long calculateOffset() {
		try {
			return Long.parseLong(params.getParams().get(2));
		} catch (NumberFormatException e) {
			String msg = "/splinesV1BetaBigDec/{type}/{interval}/{epochOffset} ; epochOffset is not a long";
			throw new BadRequest(msg);
		}
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
		if(log.isInfoEnabled())
			log.info("range="+rangeFromOffsetToStart+" offsetFromStart="+offsetFromStart+" startTime="+startTime+" result="+result);
		return result;
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
		long time = getTime(row);
		
		TSRelational clone = (TSRelational) row.clone();
		buffer.add(clone);
		if (buffer.size() == buffer.maxSize()) {
			isSplineCreated = false;

			TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
			TSRelational secondToLastVal;
			secondToLastVal = objects[objects.length - 2];
			TSRelational secondInBuf = objects[1];

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
		
		Map<String, BigDecimal> values = calculateAllValues(currentTimePointer);
		out = pointOut(currentTimePointer, values);
		log.error("-------- returning a point t="+out.getRow().getTime()+" other points are timeSecond="+timeSecondInBuffer+" timeThird="+timeThirdInBuffer);
		currentTimePointer += interval;
		return out;
	}

	private Map<String, BigDecimal> calculateAllValues(long currentTimePointer) {
		HashMap<String, BigDecimal> results = new HashMap<String, BigDecimal>();
		for (String colName:columnsToInterpolate) {
			results.put(colName, spline.get(colName).getValue(currentTimePointer));
		}
		return results;
	}

	private TSRelational figureOutRowToCopy() {
		// TODO Auto-generated method stub
		if (uninterpalatedValueMethod==UninterpalatedValueMethod.PREVIOUS_ROW) {
			TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
			return (TSRelational)objects[1].clone();
		}
		else {
			TSRelational[] objects = (TSRelational[])buffer.toArray(new TSRelational[]{});
			TSRelational secondToLastVal = objects[objects.length - 2];
			TSRelational secondInBuf = objects[1];
			if (Math.abs(secondInBuf.getTime()-currentTimePointer) > (secondToLastVal.getTime()-currentTimePointer))
				return (TSRelational)secondToLastVal.clone();
			else 
				return (TSRelational)secondInBuf.clone();
		}
	}

	private ReadResult pointOut(long time, Map<String, BigDecimal> values) {
		TSRelational rowout = figureOutRowToCopy();
		setTime(rowout, time);
		for (Map.Entry<String, BigDecimal> entry:values.entrySet()) {
			Object o = rowout.get(entry.getKey());
			if (o==null)
				throw new RuntimeException("A column name was specified for a column that does not exist!  The column name is "+entry.getKey());
			rowout.put(entry.getKey(), spline.get(entry.getKey()).getValue(currentTimePointer));
		}
		return new ReadResult(getUrl(), rowout);
	}

	// all code past here should be exactly the same as the SplinesProcessor
	// code
//	private void initialize(TSRelational row) {
//		long time = getTime(row);
//		BigDecimal value = getValueEvenIfNull(row);
//		TimeValue first = toTimeValue(time, value);
//		TimeValue veryFirst = new TimeValue(currentTimePointer,
//				first.getValue());
//		buffer.add(veryFirst);
//		// double up the first point if the first point in rows is not the same
//		// as the currentTimePointer
//		if (currentTimePointer != first.getTime()) {
//			buffer.add(veryFirst);
//		}
//	}


	private void createSpline() {
		for (String colName:columnsToInterpolate) {
			TSRelational[] array = (TSRelational[])buffer.toArray(new TSRelational[]{});
			long[] times = new long[buffer.maxSize()];
			BigDecimal[] values = new BigDecimal[buffer.maxSize()];
			for (int i = 0; i < buffer.maxSize(); i++) {
				TSRelational tv = (TSRelational) array[i];
				times[i] = tv.getTime();
				values[i] = (BigDecimal) (tv.get(colName) == null ? BigDecimal.valueOf(0.0) :tv.get(colName));
			}
			spline.get(colName).setRawDataPoints(times, values);
		}
	}
	
	private enum UninterpalatedValueMethod {
		PREVIOUS_ROW,NEAREST_ROW
	}

	// TSRelational row = new TSRelational();
	// setValue(row, val);
	// setTime(row, val);
	// new ReadResult(getUrl(),row);
}


