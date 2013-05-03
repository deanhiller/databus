package controllers.modules;

import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.util.HttpBadResponse;
import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Response;

public class CleanerMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(CleanerMod.class);

	public static void error(String path) {
		badRequest("Your url is invalid as you need rangecleanV1/{min}/{max}/{nextmodule}... and min and max have to be longs");
	}
	
	public static void go(long min, long max, String path) throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);
		
		String param = request.params.get("callback");
		
		ProcessorImpl processor = new ProcessorImpl(min, max, param);
		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while(latch.getCount() != 0 && !info.isInFailure()) {
			await(promise);
			ModuleStreamer.processResponses(info, processor);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls="+urls+" total time="+total+" ms");
	}
	
	public static class ProcessorImpl implements Processor {
		
		private long min;
		private long max;
		private String param;
		private Throwable cause=null;
		
		public ProcessorImpl(long min, long max, String param2) {
			this.min = min;
			this.max = max;
			this.param = param2;
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
		public void incomingJsonRow(boolean isFirstChunk, JsonRow row) {
			writeChunk(row, isFirstChunk);
		}

		private void writeChunk(JsonRow row, boolean isFirstChunk) {
			Response response = Response.current();
			List<TimeValue> rows = row.getRows();
			
			boolean wroteFirstRow = false;
			String resultingJson = "";
			for(int i = 0; i < rows.size(); i++) {
				TimeValue r = rows.get(i);
				Double value = r.getValue()==null?null:r.getValue().doubleValue();
				if(value == null)
					continue; //drop null on the floor
				else if(value < min || value > max)
					continue; //drop values outside the range on the floor
				
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
