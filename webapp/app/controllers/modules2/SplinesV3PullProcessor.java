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

import models.message.ChartVarMeta;

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
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class SplinesV3PullProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);
	private String splineType;
	private long epochOffset;
	protected long interval;
	private List<String> columnsToInterpolate;

	private List<ColumnState> columns = new ArrayList<ColumnState>();
	private UninterpalatedValueMethod uninterpalatedValueMethod = UninterpalatedValueMethod.PREVIOUS_ROW;
	private long currentTimePointer;
	private ReadResult lastValue;

	private CircularFifoBuffer master;
	private long end;

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, true, "Spline(version 3)");
	
	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Interval");
		meta1.setNameInJavascript("interval");
		meta1.setDefaultValue("60000");
		meta1.setClazzType(Integer.class);
		ChartVarMeta meta2 = new ChartVarMeta();
		meta2.setLabel("Epoch Offset");
		meta2.setNameInJavascript("epochOffset");
		meta2.setClazzType(Integer.class);
		ChartVarMeta meta3 = new ChartVarMeta();
		meta3.setLabel("Max To Stop Splining");
		meta3.setNameInJavascript("maxToStopSplining");
		meta3.setDefaultValue("5");
		meta3.setClazzType(Integer.class);
		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Buffer Size");
		meta.setNameInJavascript("bufferSize");
		meta.setDefaultValue("20");
		meta.setClazzType(Integer.class);
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		parameterMeta.put(meta2.getNameInJavascript(), meta2);
		parameterMeta.put(meta3.getNameInJavascript(), meta3);
		parameterMeta.put(meta.getNameInJavascript(), meta);
		
		metaInfo.setDescription("This module takes data from the source module and translates the time/value pairs into aligning with a specific time interval using spline interpolation(so you can add values with same timestamp, etc)");
	}

	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}

	@Override
	protected int getNumParams() {
		return 0;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);
		columnsToInterpolate=Arrays.asList(new String[]{valueColumn});
		String columnsToInterpolateString = options.get("columnsToInterpolate");
		if (StringUtils.isNotBlank(columnsToInterpolateString)) {
			columnsToInterpolate = Arrays.asList(StringUtils.split(columnsToInterpolateString, ";"));
		}
		String uninterpalatedValueMethodString = options.get("uninterpalatedValueMethod");
		if (StringUtils.equalsIgnoreCase("nearest", uninterpalatedValueMethodString))
			uninterpalatedValueMethod = UninterpalatedValueMethod.NEAREST_ROW;

		// param 2: Interval: long
		String intervalStr = fetchProperty("interval", "60000", options);
		try {
			interval = Long.parseLong(intervalStr);
			if (interval < 1) {
				String msg = "/splinesV2(interval="+interval+")/ ; interval must be > 0 ";
				throw new BadRequest(msg);
			}
		} catch (NumberFormatException e) {
			String msg = "/splinesV3(interval="+intervalStr+")/ ; interval is not a long ";
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

		String multipleOfInterval = fetchProperty("maxToStopSplining", "5", options);
		int maxNumIntervalsStopSplining = Integer.parseInt(multipleOfInterval);
		long maxTimeToStopSplining = interval * maxNumIntervalsStopSplining;

		String bufferSizeStr = fetchProperty("bufferSize", "20", options);
		int bufferSize = Integer.parseInt(bufferSizeStr);
		if(bufferSize >= 1000)
			throw new BadRequest("bufferSize is too large.  must be less than 1000.  size="+bufferSize);
		else if(bufferSize < 0)
			throw new BadRequest("bufferSize is too small. must be 0 or greater. size="+ bufferSize);
		
		master = new CircularFifoBuffer(4+bufferSize);

		//USE CASES
		//#1 What if one columns buffer is A1,A2,A3,NNNNNNN
		//#2 What is one columns buffer is that of #1 but with an A4 that is 1000 rows away(we don't want to read all the data in)
		//#3 
		//use windowFilterSize as max gap as well seems a bit wrong?  Have maxgap variable with default at 10 intervals

		//1    2    3    4    5   6    7    8
		//A1   A2   A3   N    N   N    N    A4
		//B1   B2   B3   B4   N   B5   B6   B7
		//
		//
		//125         890
		//1256        78N0
		//1234        567890
		//1234567890
		//
		//125
		//1256
		//2345  then 3456

		splineType = fetchProperty("splineType", "basic", options);

		/**
		 * Current spline options: basic -> SplinesBigDecBasic limitderivative
		 * -> SplinesBigDecLimitDerivative
		 */
		if ("basic".equals(splineType)) {
			for (String colname:columnsToInterpolate)
				columns.add(new ColumnState(new SplinesBigDecBasic(), timeColumn, colname, bufferSize, maxTimeToStopSplining));
		} else if ("limitderivative".equals(splineType)) {
			for (String colname:columnsToInterpolate)
				columns.add(new ColumnState(new SplinesBigDecLimitDerivative(), timeColumn, colname, bufferSize, maxTimeToStopSplining));
		} else if ("linear".equals(splineType)) {
			for (String colname:columnsToInterpolate)
				columns.add(new ColumnState(new SplinesLinear(), timeColumn, colname, bufferSize, maxTimeToStopSplining));
		} else {
			// fix this bad request line
			String msg = "/splinesV3(type="+splineType+")/ ; type must be basic or limitderivative or linear";
			throw new BadRequest(msg);
		}

		return newPath;
	}

	protected long calculateOffset() {
		Long startTime = params.getStart();
		long offset = startTime % interval;
		return offset;
	}

	protected long parseOffset(String offset) {
		try {
			return Long.parseLong(offset);
		} catch (NumberFormatException e) {
			String msg = "/splinesV3(epochOffset="+offset+")/ epochOffset is not a long";
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
				return calculateLastRows(lastValue);
			} else {
				transferRow(lastValue.getRow());
			}
		}

		//FIRST is move currentTimePointer so currentTime > 2nd point time(of 4 point spline) for ONE
		//of the columns!!!  We don't want to return any nulls since urls that overlap should always return the 
		//same values(ie. can't return null here when it is just a fact we don't have enough data)
		//We need at least one of the streams to have currentTimePointer after the 2nd point so can spline that
		//ONE guy at that time point(and the other ones would have to just return null
		while(currentTimeLessThan2ndPtAndBufferFull(currentTimePointer)) {
			incrementCurrentTime();
		}

		//needMoreData is a very tricky method so read the comments in that method
		while(needMoreData() && currentTimePointer <= end) {
			pull();
			if (lastValue == null) {
				return null;
			} else if (lastValue.isMissingData()) {
				return lastValue;
			} else if (lastValue.isEndOfStream()) {
				return calculateLastRows(lastValue);
			} else {
				transferRow(lastValue.getRow());
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

	private ReadResult calculateLastRows(ReadResult lastValue) {
		if(currentTimePointer > end) {
			return lastValue;
		}
		
		//otherwise we still need to calculate the splines
		boolean justOneCanSpline = false;
		for(ColumnState s : columns) {
			boolean canSpline = s.prepareBuffer(currentTimePointer);
			if(canSpline)
				justOneCanSpline = true;
		}

		if(justOneCanSpline)
			return calculate();
		return lastValue;
	}

	private boolean currentTimeLessThan2ndPtAndBufferFull(long currentTimePointer2) {
		for(ColumnState s : columns) {
			if(s.getBuffer().isFull() && !s.secondPointGreaterThan(currentTimePointer2))
				return false; //If anyone viol
		}
		return true;
	}

	private boolean needMoreData() {
		//1st, if currentTimePoint is less than 3rd timepoint for EVERY column, we don't need more data(
		//2nd, if currentTimePoint is is between 2nd and 3rd on stream A BUT before 2nd on stream B, we need more data ONLY IF
		//          stream A does not have a full buffer
		//if case2 is stream A has a full buffer and currentTime < stream B 2nd point, then stream B will not be able to do a 
		//spline and will have to return null, otherwise pulling more data just puts more in buffer(leftOver) for stream A while adding to
		//the spline buffer of stream B(instead of leftOver buffer)

		//CASE #1....
		Set<ColumnState> columnThatNeedsMore = new HashSet<ColumnState>();
		Set<ColumnState> columnsNotNeedingMore = new HashSet<ColumnState>();
		for(ColumnState s : columns) {
			//IF we can't spline with ANY SINGLE stream(needMore=true), we need to move on and check use CASE #2 below
			if(s.needMoreData(currentTimePointer))
				columnThatNeedsMore.add(s);
			else
				columnsNotNeedingMore.add(s);
		}
		//if 
		if(columnThatNeedsMore.size() == 0)
			return false; //no columns need more data, yeah!!!
		else if(columnThatNeedsMore.size() == columns.size())
			return true; //all columns need more data, yeah!!!! (easy case)

		//At this point, we have N columns that need more data
		//Use CASE #2.  we need more data ONLY IF stream A, B, C does not have a full buffer 
		for(ColumnState s : columnsNotNeedingMore) {
			//If any of the columns NOT needing data have a full leftover buffer, we have to wait on those
			//columns leftOver buffer to shrink before we need more rows to be read in..
			if(s.isLeftOverBufferFull())
				return false; 
		}

		return true;
	}

	private boolean anyStreamIsReady() {
		for(ColumnState s : columns) {
			if(s.getBuffer().isFull())
				return true;
		}
		//none of the streams are full...we only need one to be full to be ready
		return false;
	}

	private void transferRow(TSRelational row) {
		master.add(row);
		for(ColumnState s : columns) {
			s.transferRow(row, currentTimePointer);
		}
	}

	private void pull() {
		PullProcessor ch = getChild();
		lastValue = ch.read();
	}

	private ReadResult calculate() {
		TSRelational row = new TSRelational();
		TSRelational r = findProperRow();
		//copy the row....(and then we overwrite it with proper values)
		for(Entry<String, Object> entry : r.entrySet()) {
			row.put(entry.getKey(), entry.getValue());
		}

		setTime(row, currentTimePointer);
		for(ColumnState s : columns) {
			s.calculate(row, currentTimePointer);
		}
		incrementCurrentTime();
		
		return new ReadResult(getUrl(), row);
	}

	private void incrementCurrentTime() {
		currentTimePointer += interval;
		TSRelational[] rows = (TSRelational[]) master.toArray(new TSRelational[0]);
		if(rows.length >= 2) {
			long time = getTime(rows[1]);
			if(currentTimePointer > time)
				master.remove(rows[0]); //remove first row, we don't need it if currentTime > row 2's time
		}
	}

	private TSRelational findProperRow() {
		//since in incrementCurrentTime method, we always remove from the master buffer, we "should" be able
		//to just use the first row and second row!!!
		TSRelational[] rows = (TSRelational[]) master.toArray(new TSRelational[0]);
		long t1 = getTime(rows[0]);
		if(currentTimePointer < t1) {
			if(uninterpalatedValueMethod == UninterpalatedValueMethod.PREVIOUS_ROW)
				return new TSRelational();
			else
				return rows[0];
		}
		
		if(uninterpalatedValueMethod == UninterpalatedValueMethod.PREVIOUS_ROW)
			return rows[0];

		long t2 = getTime(rows[1]);
		long diff1 = currentTimePointer - t1;
		long diff2 = t2-currentTimePointer;
		if(diff1 < diff2)
			return rows[0];
		return rows[1];
	}

	private enum UninterpalatedValueMethod {
		PREVIOUS_ROW,NEAREST_ROW
	}

}
