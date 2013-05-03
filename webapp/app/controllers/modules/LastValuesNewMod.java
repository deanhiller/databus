package controllers.modules;

import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Response;
import controllers.modules.lastValuesMod.ProcessorImpl;
import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;
import controllers.modules.util.OldSuccessfulAbort;

public class LastValuesNewMod extends Controller {
	
	public static final Logger log = LoggerFactory.getLogger(CleanerMod.class);
	
	public static void error(String path) {
		badRequest("Your url is invalid as you need lastvaluesV1/{number}/{nextmodule}... and number has to be a long");
	}
	
	public static void lastValues(long number, String path)
			throws InterruptedException, ExecutionException, IOException {
		String newPath = formEndOFPath(path);
		List<String> urls = new ArrayList<String>();
		urls.add(newPath);

	    String param = request.params.get("callback");
	    
		Processor p;
		if (request.isNew) {	
			p = new ProcessorImpl(number, param);
			request.args.put("processor", p);
		} else {
			p = (Processor) request.args.get("processor");
		}

		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while (latch.getCount() != 0 && !info.isInFailure()) {
			await(promise);
			ModuleStreamer.processResponses(info, p);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}

	private static String formEndOFPath(String path) {
		String[] split = path.split("/");
		String to = split[split.length-1];
		String from = split[split.length-2];
		try {
			Long.parseLong(to);
		} catch(NumberFormatException e) {
			//there is no times, so let's create our own
			path += "/"+Long.MIN_VALUE+"/"+System.currentTimeMillis();
			return path;
		}
		
		try {
			Long.parseLong(from);
		} catch(NumberFormatException e) {
			badRequest("You must have a start and end time OR no start and end time");
		}
		//good, they already have a start and end time in mind so use theirs
		return path;
	}

	public static class ProcessorImpl implements Processor {

		private long counter;
		private long numOfLastVals;
		private String param;
		private Throwable cause=null;
		private boolean headerWrittenAlready = false;
		private boolean isFirstLine = true;

		public ProcessorImpl(long number, String param) {
			this.numOfLastVals = number;
			this.param = param;
		}

		@Override
		public void onStart() {
			ModuleStreamer.writeHeader(param);
		}

		@Override
		public void complete(String url) {
			if(!headerWrittenAlready) {
				ModuleStreamer.writeHeader(param);
				headerWrittenAlready = true;
			}
		}

		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow row) {
			Response response = Response.current();
			List<TimeValue> rows = row.getRows();
			
			String resultingJson = "";
			for(int i = 0; i < rows.size(); i++) {
				TimeValue r = rows.get(i);
				if(isFirstLine ) {
					isFirstLine = false;
				} else {
					resultingJson += ",";
				}
				resultingJson = ModuleStreamer.translateToString(resultingJson, r);
				counter++;
				if(counter >= numOfLastVals) {
					response.writeChunk(resultingJson);		
					complete(null); //write the header here
					//NOW close the client stream as well...
					
					throw new OldSuccessfulAbort("aborting as we are done");
				}
			}
		}

		@Override
		public void onFailure(String url, Throwable exception) {
			if (log.isWarnEnabled())
        		log.warn("Exception for url=" + url, exception);
			// let's stop processing all streams on failure...
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
