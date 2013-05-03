package controllers.modules;

import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Response;

public class StdDevMod extends Controller {

	public static final Logger log = LoggerFactory.getLogger(StdDevMod.class);

	public static void runStdDev(int rowCount, double factor, String path)
			throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);

		String param = request.params.get("callback");
		
		Processor p;
		if(request.isNew) {
			
			p = new ProcessorImpl(rowCount, factor, param);
			request.args.put("processor", p);
		} else {
			p = (Processor) request.args.get("processor");
		}

		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while (latch.getCount() != 0) {
			if (log.isDebugEnabled())
				log.debug("await for more stuff");
			await(promise);
			if (log.isDebugEnabled())
				log.debug("firing into ModuleStreamer from StdDevMod");
			ModuleStreamer.processResponses(info, p);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}

	public static class ProcessorImpl implements Processor {

		private StdDevCalculations stdDev;
		private String param;
		private double factor;
		private Throwable cause=null;
		
		public ProcessorImpl(int rowCount, double factor, String param) {
			this.factor = factor;
			stdDev = new StdDevCalculations(rowCount);
		}

		@Override
		public void onStart() {
			ModuleStreamer.writeHeader(param);
		}

		@Override
		public void complete(String url) {
			ModuleStreamer.writeFooter(param, getCause());
		}

		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow chunk) {
			StatisticalSummary stats = stdDev.processSingleChunk(chunk.getRows());
			writeAChunk(chunk, stats.getMean(), stats.getStandardDeviation(), factor, isFirstChunk);
		}

		private void writeAChunk(JsonRow row, double mean, double std, double factor, boolean isFirstChunk) {
			Response response = Response.current();
			List<TimeValue> rows = row.getRows();
			
			double allowedVariance = std*factor;
			double min = mean-allowedVariance;
			double max = mean+allowedVariance;
			boolean wroteFirstRow = false;
			String resultingJson = "";
			for(int i = 0; i < rows.size(); i++) {
				TimeValue r = rows.get(i);
				Double value = r.getValue()==null?null:r.getValue().doubleValue();
				//TODO:  should we count null as zero instead?  What does null mean here?
				if(value == null)					
					continue; 
				else if(value < min || value > max)					
					continue; //drop values outside the range
				boolean isFirstRow = isFirstChunk && !wroteFirstRow;
				if(isFirstRow) {
					wroteFirstRow = true;
				} else {
					resultingJson += ",";
				}
				resultingJson = ModuleStreamer.translateToString(resultingJson, r);
			}

			if(resultingJson.length() > 0) 
				response.writeChunk(resultingJson);
		}

		@Override
		public void onFailure(String url, Throwable exception) {
			if (log.isWarnEnabled())
        		log.warn("Exception for url="+url, exception);
			//let's stop processing all streams on failure...
			throw new RuntimeException("failure", exception);
		}
		
		public Throwable getCause() {
			return cause;
		}

		public void setCause(Throwable cause) {
			this.cause = cause;
		}
	}

}
