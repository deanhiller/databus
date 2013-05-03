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

public class NullMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(NullMod.class);

	public static void redirectToRawdata(String table, Long start, Long end) throws InterruptedException, ExecutionException, IOException {
		NullMod.goImpl("rawdataInnerV1/"+table+"/"+start+"/"+end);
	}
	
	public static void go(String path) throws InterruptedException, ExecutionException, IOException {
		NullMod.goImpl(path);
	}
	
	private static void goImpl(String path) throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);
		
		String param = request.params.get("callback");
		
		ProcessorImpl processor = new ProcessorImpl(param);
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
		
		private String param;
		private Throwable cause=null;
		
		public ProcessorImpl(String param2) {
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
				if(value != null && isMaxInt(value)) {
					r.setValue(null);
				}
				
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

		public static void main(String[] args) {
			double d = Integer.MAX_VALUE;
			String s = ""+d;
			double newD = Double.parseDouble(s);
			boolean t = isMaxInt(newD);
		}
		
		private static boolean isMaxInt(Double value) {
			try {
				double d = value;
				int val = (int) d;
				if(Integer.MAX_VALUE == val)
					return true;
			} catch(NumberFormatException e) {
				return false;
			}
			return false;
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
