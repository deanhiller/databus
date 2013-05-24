package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.results.BadRequest;

import controllers.modules.SplinesBigDecBasic;
import controllers.modules.SplinesBigDecLimitDerivative;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;

// .../datafiller/valueToFill/interval/epochOffset/...
public class DataFillerProcessor  extends PullProcessorAbstract {
	private static final Logger log = LoggerFactory.getLogger(DataFillerProcessor.class);
	
	protected long interval;
	
	private long currentTimePointer;
	private BigDecimal valueToFill;

	private ReadResult lastValue;
	private long epochOffset;
	
	@Override
	protected int getNumParams() {
		return 3;
	}
	
	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of datafiller processor");
		String newPath = super.init(path, nextInChain, visitor, options);
		// param 1: valueToFill: String
		String valueToFillAsString = params.getParams().get(0);
	
		if ("null".equalsIgnoreCase(valueToFillAsString)) {
			valueToFill = null;
		} else {
			try {
				valueToFill = new BigDecimal(valueToFillAsString);
			}
			catch (NumberFormatException nfe) {
				throw new BadRequest("datafillerV1 requires a value to fill in as the first parameter that can be 'null' or a valid decimal number");
			}
		}

		// param 2: Interval: long
		try {
			interval = Long.parseLong(params.getParams().get(1));
			if (interval < 1) {
				String msg = "/datafillerV1/{valueToFill}/{interval}/{epochOffset} ; interval must be > 0 ";
				throw new BadRequest(msg);
			}
		} catch (NumberFormatException e) {
			String msg = "/datafillerV1/{valueToFill}/{interval}/{epochOffset} ; interval is not a long ";
			throw new BadRequest(msg);
		}

		//param 3: epochOffset: long
		epochOffset = calculateOffset();

		if(params.getStart() == null || params.getEnd() == null) {
			String msg = "datafillerV1 must have a start and end (if you want it to work, request it)";
			throw new BadRequest(msg);
		}
		
		Long startTime = params.getStart();
		if(log.isInfoEnabled())
			log.info("offset="+epochOffset+" start="+startTime+" interval="+interval);
		currentTimePointer = calculateStartTime(startTime, interval, epochOffset);
		
		return newPath;
	}
	
	protected long calculateOffset() {
		try {
			return Long.parseLong(params.getParams().get(2));
		} catch (NumberFormatException e) {
			String msg = "/datafillerV1/{valueToFill}/{interval}/{epochOffset} ; epochOffset is not a long";
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
		if (currentTimePointer > params.getEnd())
			return new ReadResult();
		while (lastValue == null)
			pull();
		return calculatePoint();
	}
	
	private ReadResult calculatePoint() {
		ReadResult result;
		if (lastValue.isEndOfStream() || currentTimePointer < lastValue.getRow().getTime()) {
			result = new ReadResult(getUrl(), new TSRelational(new BigInteger(""+currentTimePointer), valueToFill));
			incCTP();
		}
		else {
			result = lastValue;
			if (currentTimePointer == lastValue.getRow().getTime())
				incCTP();
			pull();
		}
		
		
		return result;
	}

	
	private void incCTP() {
		currentTimePointer += interval;
	}

	private void pull() {
		PullProcessor ch = getChild();
		ReadResult r = null;
		while (r == null || r.isMissingData())
			r = ch.read();
		lastValue = r;
	}

}
