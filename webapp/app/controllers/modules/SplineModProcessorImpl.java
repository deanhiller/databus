package controllers.modules;

import gov.nrel.util.TimeValue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import play.mvc.Http.Response;

import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.Processor;

public class SplineModProcessorImpl implements Processor {

	private boolean isFirstRow = true;
	private SplineCalculations splines;
	private String param;
	private long pointer;
	private long interval;
	private Throwable cause=null;
	private Response response;
	
	public SplineModProcessorImpl(long interval, int windowSize, long start, long end, String param, Response response) {
		CircularFifoBuffer buffer = new CircularFifoBuffer(windowSize);
		splines = new SplineCalculations(buffer, start, end, interval);
		this.interval = interval;
		this.pointer = start;
		this.param = param;
		this.response = response;
	}

	@Override
	public void onStart() {
		ModuleStreamer.writeHeader(param);
	}

	@Override
	public void complete(String url) {
		List<TimeValue> interpolatedTimes = new ArrayList<TimeValue>();
		splines.processLeftOver(interpolatedTimes);
		writeAChunk(interpolatedTimes);
		ModuleStreamer.writeFooter(param, getCause());
	}

	@Override
	public void incomingJsonRow(boolean isFirstChunk, JsonRow chunk) {
		if(SplineMod.log.isDebugEnabled())
			SplineMod.log.debug("ticket 42661899 - entering SplineMod.incomingJsonRow, chunk has "+chunk.getRows().size()+" rows.");
		List<TimeValue> interpolatedTimes = new ArrayList<TimeValue>();
		splines.processSingleChunk(chunk.getRows(), interpolatedTimes );
		if(SplineMod.log.isDebugEnabled())
			SplineMod.log.debug("ticket 42661899 - in SplineMod.incomingJsonRow, about to writeAChunk, interpolatedTimes has "+interpolatedTimes.size()+" values.");
		writeAChunk(interpolatedTimes);
	}

	private void writeAChunk(List<TimeValue> interpolatedTimes) {
		String resultingJson = "";
		if(SplineMod.log.isDebugEnabled())
			SplineMod.log.debug("ticket 42661899 - entering SplineMod.writeAChunk, interpolatedTimes has "+interpolatedTimes.size()+" values.");
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
			
			pointer+=interval;
			if(SplineMod.log.isDebugEnabled())
				SplineMod.log.debug("ticket 42661899 - in SplineMod.writeAChunk, resultingJson is "+resultingJson.length()+" chars long.");
			resultingJson = ModuleStreamer.translateToString(resultingJson, tv);
		}	

		if(resultingJson.length() > 0) 
			response.writeChunk(resultingJson);
	}

	@Override
	public void onFailure(String url, Throwable exception) {
		if (SplineMod.log.isWarnEnabled())
    		SplineMod.log.warn("Exception for url="+url, exception);
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