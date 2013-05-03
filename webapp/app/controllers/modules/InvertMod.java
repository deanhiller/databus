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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.CleanerMod.ProcessorImpl;
import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Response;

public class InvertMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(CleanerMod.class);

	public static void go(long interval, int windowSize, String path)
			throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);

		String param = request.params.get("callback");
		
		ProcessorImpl processor = new ProcessorImpl(param);
		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while(latch.getCount() != 0) {
			await(promise);
			ModuleStreamer.processResponses(info, processor);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}

	public static class ProcessorImpl implements Processor {

		private boolean isFirstRow = true;
		private String param;
		private Throwable cause=null;
		
		public ProcessorImpl(String param) {
			this.param = param;
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
			String resultingJson = "";
			for(TimeValue tv : chunk.getRows()) {
				BigDecimal val = tv.getValue();
				if(val != null) {
					BigDecimal newVal = val.negate();
					tv.setValue(newVal);
				}
				
				if(isFirstRow) {
					isFirstRow = false;
				} else {
					resultingJson += ",";
				}
				resultingJson = ModuleStreamer.translateToString(resultingJson, tv);
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
