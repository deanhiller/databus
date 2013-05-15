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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Response;

public class TimeAverageMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(CleanerMod.class);

	public static void error(String path) {
		badRequest("Your url is invalid as you need timeaverageV1/{type}/{nextmodule}... and type has to be a long");
	}
	
	public static void timeAggregation(long type, String path)
			throws InterruptedException, ExecutionException, IOException {
		
		// String nextUrl = ModuleStreamer.formNextPath(path);
		List<String> urls = new ArrayList<String>();
		urls.add(path);

	    String param = request.params.get("callback");
	    
		Processor p;
		if (request.isNew) {
			PathParse pathParse = new PathParse();
			long[] startEnd = pathParse.calculateStartEnd(path);
			
			p = new ProcessorImpl(type, startEnd[0], startEnd[1], param);
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

	public static class ProcessorImpl implements Processor {

		private long interval;
		private long lastStart = 0l;
		private long start;
		private long end;
		private boolean isFirstRow = true;
		private Integer count = 0;
		private Double total = 0d;
		private String param;
		private Throwable cause=null;
		private long lastRowTime = Long.MIN_VALUE;
		
		public ProcessorImpl(long type, long start, long end, String param) {
			this.interval = type;
			this.start = start;
			this.end = end;
			this.param = param;
			
			this.lastStart = this.start;
		}

		@Override
		public void onStart() {
			ModuleStreamer.writeHeader(param);
		}

		@Override
		public void complete(String url) {
			if( this.end >= lastStart + interval - 1){
				//write out last data point if we should have gotten the data for the interval
				String resultingJson = "";
				if (count != 0) {
					double d = total / count.doubleValue();
					// write out new value
					TimeValue newRow = new TimeValue(lastStart, BigDecimal.valueOf(d));

					if (isFirstRow) {
						isFirstRow = false;
					} else {
						resultingJson += ",";
					}
					resultingJson = ModuleStreamer.translateToString(
							resultingJson, newRow);
				}
				if (resultingJson.length() > 0)
					response.writeChunk(resultingJson);
			}
			ModuleStreamer.writeFooter(param, getCause());
		}

		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow row) {
			writeChunk(row, isFirstChunk);
		}

		private void writeChunk(JsonRow lastRows, boolean isFirstChunk) {
			List<TimeValue> rows = lastRows.getRows();
			if(rows.isEmpty())
				return;
			
//			if (isFirstRow) {
//				long time = rows.get(0).getTime();
//				if (time % type == 0) {
//					lastStart = time;
//				} else {
//					lastStart = (time - (time % type));
//				}
//			}

			// add all the new rows
			Response response = Response.current();

			// process until current
			String resultingJson = "";
			int rowPointer = 0;
			while (rowPointer < rows.size()) {
				TimeValue tv = rows.get(rowPointer);
				
				long nextStart = lastStart+interval;
				if (tv.getTime() >= nextStart) {

					if (count != 0) {
						double d = total / count.doubleValue();
						// write out new value
						TimeValue newRow = new TimeValue(lastStart, BigDecimal.valueOf(d));

						if (isFirstRow) {
							isFirstRow = false;
						} else {
							resultingJson += ",";
						}
						resultingJson = ModuleStreamer.translateToString(
								resultingJson, newRow);

						for (int i = 0; i < rowPointer; i++) {
							rows.remove(0);
						}
					}
					
					total = 0d;
					count = 0;
					rowPointer = 0;
					lastStart += interval;
				} else if (tv.getTime() >= lastStart) {
					//if value is null it means something below us returned NaN or Infinite or similar.  We are dropping this for now:
					if (tv.getValue() != null) {
						// add to total
						total += tv.getValue().doubleValue();
						count++;
					}
					rowPointer++;
				} else {
					rows.remove(0);
					throw new RuntimeException("(our problem) should never drop data in TimeAverageMod. time="+tv.getTime()+" lastStart="+lastStart+" nextStart="+nextStart+" lastrowtime="+lastRowTime);
				}
				lastRowTime = tv.getTime();
			}

			if (resultingJson.length() > 0)
				response.writeChunk(resultingJson);
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
