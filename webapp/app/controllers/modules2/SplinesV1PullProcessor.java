package controllers.modules2;

import java.math.BigDecimal;

import gov.nrel.util.TimeValue;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
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

public class SplinesV1PullProcessor extends SplinesPullProcessor {

	@Override
	protected int getNumParams() {
		return 2;
	}

	protected long calculateOffset() {
		Long startTime = params.getStart();
		long offset = startTime % interval;
		return offset;
	}

}
