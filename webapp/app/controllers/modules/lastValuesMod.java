package controllers.modules;

import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.auth.GuiSecure;
import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;

public class lastValuesMod extends Controller {

	public static final Logger log = LoggerFactory.getLogger(CleanerMod.class);

	public static void error(String path) {
		badRequest("Your url is invalid as you need lastvaluesV1/{number}/{nextmodule}... and number has to be a long");
	}
	
	public static void lastValues(long number, String path)
			throws InterruptedException, ExecutionException, IOException {

		String param = request.params.get("callback");

		List<String> urls = new ArrayList<String>();
		boolean firstTime = true;		
		ProcessorImpl p = null;
		boolean errorOccurred = false;
		String currentSessionUser = Session.current().get(GuiSecure.KEY);
		
		while (p == null || p.numLastVals() < number) {
			urls = new ArrayList<String>();
			
			String[] pathPieces = path.split("/");
			long now = System.currentTimeMillis();
			if(firstTime) {
				try {
					long end;
					while(Long.valueOf(pathPieces[pathPieces.length-1]) != null) {
						String[] newPathPieces = new String[pathPieces.length-1];
						for(int i = 0; i < pathPieces.length-1; i++) {
							newPathPieces[i] = pathPieces[i];
						}
						pathPieces = newPathPieces;
					}
				} catch(NumberFormatException e) {
					// all good 
				}
				
				path = "";
				for(int i = 0; i < pathPieces.length; i++) {
					path += pathPieces[i] + "/";
				}
				
				if(now > 100000) {
					path += (now - 100000) + "/" + now;
				} else {
					path += 0 + "/" + now;					
				}
				
				firstTime = false;				
			} else {
				long end = Long.valueOf(pathPieces[pathPieces.length-1]);
				long start = Long.valueOf(pathPieces[pathPieces.length-2]);
				
				if(start == 0l) {
					break;
				}
				
				String newPath = "";
				for(int i = 0; i < pathPieces.length-2; i++) {
					newPath += pathPieces[i] + "/";
				}
				
				start = (now-(end-start+10)*10);
				if(start < 0) {
					start = 0;
				}
				
				path = newPath + start + "/" + now;
			}
			if (log.isInfoEnabled())
				log.info("****" + path);
			urls.add(path);
			
			if(request.isNew) { 
				p = new ProcessorImpl(number, param);
				request.args.put("processor", p);
			} else {
				p = (ProcessorImpl) request.args.get("processor");
				if (StringUtils.isBlank(Session.current().get(GuiSecure.KEY)))
					Session.current().put(GuiSecure.KEY, currentSessionUser);
			}

			//Request.current().isNew = true;
			Info info = ModuleStreamer.startStream(urls, "", true);
			CountDownLatch latch = info.getLatch();
			OurPromise<Object> promise = info.getPromise();

			while (latch.getCount() != 0) {
				await(promise);
				ModuleStreamer.processResponses(info, p);
				if (info.isInFailure()) {
					errorOccurred = true;
					break;
				}
			}
		}
		

		Response response = Response.current();
		String resultingJson = "";
		
		Iterator<TimeValue> valIt = p.lastVals.iterator();
		while (valIt.hasNext()) {
			if(p.isFirstRow) {
				p.isFirstRow = false;
			} else {
				resultingJson += ",";
			}
			resultingJson = ModuleStreamer.translateToString(resultingJson,
					valIt.next());
		}

		if(resultingJson.length() > 0 && !errorOccurred) {
			response.writeChunk(resultingJson);
			ModuleStreamer.writeFooter(param, null);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}

	public static class ProcessorImpl implements Processor {

		private long numOfLastVals;
		private ArrayList<TimeValue> lastVals;
		private boolean isFirstRow = true;
		private boolean writeHeader = true;
		private String param;
		private Throwable cause=null;

		public int numLastVals() {
			if(lastVals == null) {
				return 0;
			} else {
				return lastVals.size();
			}
		}
		
		public ProcessorImpl(long number, String param) {
			this.numOfLastVals = number;
			lastVals = new ArrayList<TimeValue>();
			this.param = param;
		}

		@Override
		public void onStart() {
			if(writeHeader) {
				ModuleStreamer.writeHeader(param);
				writeHeader = false;
			}
		}

		@Override
		public void complete(String url) {
			if (log.isInfoEnabled())
				log.info("stream complete="+url);
		}

		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow row) {
			writeChunk(row, isFirstChunk);
		}

		private void writeChunk(JsonRow lastRows, boolean isFirstChunk) {
			Iterator<TimeValue> valIt = lastRows.getRows().iterator();
			while (valIt.hasNext()) {
				lastVals.add(valIt.next());

				if (lastVals.size() > numOfLastVals) {
					lastVals.remove(0);
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
