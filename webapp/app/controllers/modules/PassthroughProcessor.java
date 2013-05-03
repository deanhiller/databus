package controllers.modules;

import gov.nrel.util.TimeValue;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Response;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.Processor;

public class PassthroughProcessor implements Processor {
	
	private static final Logger log = LoggerFactory.getLogger(PassthroughProcessor.class);
	private String param;
	private Throwable cause=null;
	private boolean isFirstLine = true;
	
	public PassthroughProcessor(String param) {
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
	public void incomingJsonRow(boolean isFirstChunk, JsonRow row) {
		Response response = Response.current();
		List<TimeValue> rows = row.getRows();
		
		String resultingJson = "";
		for(int i = 0; i < rows.size(); i++) {
			TimeValue r = rows.get(i);
			if(isFirstLine) {
				isFirstLine = false;
			} else {
				resultingJson += ",";
			}
			resultingJson = ModuleStreamer.translateToString(resultingJson, r);
		}
		
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