package controllers.modules;

import gov.nrel.util.TimeValue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import play.mvc.Http.Response;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.Processor;

public class SplinesModProcessorImpl implements Processor {

		private boolean isFirstRow = true;
		private SplinesCalculations splines;
		private String param;
		private long pointer;
		private long interval;
		private Throwable cause=null;
		private long direction;
		
		public SplinesModProcessorImpl(long interval, int windowSize, long start, long end, String param,Splines splines) {
			CircularFifoBuffer buffer = new CircularFifoBuffer(windowSize);
			this.splines = new SplinesCalculations(buffer, start, end, interval,splines, 50);
			this.interval = interval;
			this.pointer = start;
			this.param = param;
			this.direction = Math.round(Math.signum(end-start));
		}

		@Override
		public void onStart() {
			ModuleStreamer.writeHeader(param);
		}

		@Override
		public void complete(String url) {
			List<TimeValue> interpolatedTimes = new ArrayList<TimeValue>();
			while (!splines.processLeftOver(interpolatedTimes)) {
				writeAChunk(interpolatedTimes);
				interpolatedTimes.clear();
			}
			writeAChunk(interpolatedTimes);
			interpolatedTimes.clear();
			ModuleStreamer.writeFooter(param, getCause());
		}

		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow chunk) {
			List<TimeValue> interpolatedTimes = new ArrayList<TimeValue>();
			while (!splines.processSingleChunk(chunk.getRows(), interpolatedTimes)) {
				writeAChunk(interpolatedTimes);
				interpolatedTimes.clear();
			}
			writeAChunk(interpolatedTimes);
			interpolatedTimes.clear();
		}

		private void writeAChunk(List<TimeValue> interpolatedTimes) {
			String resultingJson = "";
			for(TimeValue tv : interpolatedTimes) {
				if(isFirstRow) {
					if(pointer != tv.getTime())
						throw new RuntimeException("Bug on postcondition, should be the start time for first row. pointer="+pointer+" time="+tv.getTime());
					isFirstRow = false;
				} else {
					if(pointer != tv.getTime())
						throw new RuntimeException("Bug on postcondition, we should not be missing any times. pointer="+pointer+" time="+tv.getTime());
					resultingJson += ",";
				}
				
				pointer += interval*direction;
				resultingJson = ModuleStreamer.translateToString(resultingJson, tv);
			}	

			if(resultingJson.length() > 0){
				Response response = Response.current();
//				getResponse().writeChunk(resultingJson);
				response.writeChunk(resultingJson);
			}
		}

		@Override
		public void onFailure(String url, Throwable exception) {
			if (SplineMod.log.isWarnEnabled())
        		SplinesMod.log.warn("Exception for url="+url, exception);
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