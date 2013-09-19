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

public class GapProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(SplinesPullProcessor.class);

	private int maxBeforeInsertingNull;
	private Long previousTime;

	private ReadResult toReturnNext;

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);

		String max = fetchProperty("maxGap", "1200", options);
		maxBeforeInsertingNull = 1000*Integer.parseInt(max);

		return newPath;
	}

	@Override
	public ReadResult read() {
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

		PullProcessor ch = getChild();
		ReadResult value = ch.read();
		if(value.isEndOfStream())
			return value;
		else if(value.isMissingData())
			return value;
		
		TSRelational row = value.getRow();
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
			setTime(emptyRow, previousTime+1);
			setValue(emptyRow, null);
			ReadResult r = new ReadResult(null, emptyRow);
			return r;
		}

		return value;
	}

}
