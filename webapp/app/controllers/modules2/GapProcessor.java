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
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.ProxyProcessor;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class GapProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);

	private Long maxBeforeInsertingNull;
	private Integer maxMultiple;
	private Long previousTime;

	private ReadResult toReturnNext;

	private boolean reversed;
	private TSRelational firstDataPt;

	private ProxyProcessor readAheadProc;

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, false);

	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Max Gap");
		meta1.setNameInJavascript("maxgap");
		meta1.setHelp("If specified, this module inserts null between points that are Max Gap or more apart from each other so graphs show a line break(otherwise graphs connect the dots)");
		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Max Multiple");
		meta.setNameInJavascript("offset");
		meta.setDefaultValue("5");
		meta.setHelp("If specified, Max Gap is calculated from the first 3 data points as the minimum distance between point 1 and 2 OR point 2 and 3 and then multiplied by maxMultiple");
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		parameterMeta.put(meta.getNameInJavascript(), meta);
	}
	
	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor,
			ProcessorSetup useThisChild, boolean alreadyAddedInverter) {
		ProcessorSetup setup = super.createPipeline(path, visitor, useThisChild, alreadyAddedInverter);
		readAheadProc = new ProxyProcessor((PullProcessor) child);
		return setup;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);

		reversed = visitor.isReversed();
		String maxG = options.get("maxGap");
		if(maxG == null) {
			String maxM = fetchProperty("maxMultiple", "5", options);
			maxMultiple = Integer.parseInt(maxM);
		} else
			maxBeforeInsertingNull = 1000*Long.parseLong(maxG);

		return newPath;
	}

	@Override
	public ReadResult read() {
		//maxBeforeInsertingNull will be null until we use maxMultiple * smallest interval of first 3 points
		//and then readForMaxGap takes over for us
		if(maxBeforeInsertingNull == null)
			readForMaxMultiple();
		return readForMaxGap();
	}

	private void readForMaxMultiple() {
		//read 3 values but they may all be end of streams
		boolean endOfStream = false;
		List<ReadResult> firstThree = readAheadProc.peekAhead(3);
		for(ReadResult value1 : firstThree) {
			if(value1.isEndOfStream())
				endOfStream = true;
		}

		if(!endOfStream) {
			long t1 = fetchTime(firstThree.get(0));
			long t2 = fetchTime(firstThree.get(1));
			long t3 = fetchTime(firstThree.get(2));
			long diff1 = Math.abs(t2 - t1);
			long diff2 = Math.abs(t3 - t2);
			long min = Math.min(diff1, diff2);
			maxBeforeInsertingNull = min * maxMultiple;
		}
	}
	
	private long fetchTime(ReadResult readResult) {
		TSRelational row = readResult.getRow();
		return getTime(row);
	}

	private ReadResult readForMaxGap() {
		ReadResult res = readImpl();
		TSRelational row = res.getRow();
		if(row != null)
			previousTime = getTime(row);
		return res;
	}
	
	public ReadResult readImpl() {
		if(toReturnNext != null) {
			ReadResult retVal = toReturnNext;
			toReturnNext = null;
			return retVal;
		}

		ReadResult value = readAheadProc.read();
		if(value.isEndOfStream())
			return value;
		else if(value.isMissingData())
			return value;
		
		TSRelational row = value.getRow();
		if(firstDataPt == null)
			firstDataPt = row;

		long time = getTime(row);
		if(previousTime == null) {
			return value;
		} 

		long abs = Math.abs(time-previousTime);
		if(abs > maxBeforeInsertingNull) {
			//okay, cache this row to return next and for now return a row with a time
			//and no other columns as this is where we need to break up the line in the 
			//chart
			toReturnNext = value;
			TSRelational emptyRow = new TSRelational();
			long newTime = previousTime+1;
			if(reversed)
				newTime = previousTime-1;
			
			//at least for hicharts, this value HAS to be set to null so in the json
			//we end up with "value" : null which is required or ALL data points do not draw in hicharts
			for(Entry<String, Object> kv : firstDataPt.entrySet()) {
				String key = kv.getKey();
				emptyRow.put(key, null);
			}
			setTime(emptyRow, newTime);
			ReadResult r = new ReadResult(null, emptyRow);
			return r;
		}

		return value;
	}

}
