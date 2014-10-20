package controllers.modules2.framework.chain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.io.JsonStringEncoder;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Http.Response;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.http.HttpStatus;
import controllers.modules2.framework.procs.OutputProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class FTranslatorValuesToCsv extends PushProcessorAbstract implements OutputProcessor {

	private static final Logger log = LoggerFactory.getLogger(FTranslatorValuesToCsv.class);

	private boolean isFirstRow = true;
	private StringBuilder chunkToSend = new StringBuilder();
	private int chunkSize;
	private int rowCount = 0;
	private HttpStatus failedStatus;
	private int keyListSize = 0;

	private List<String> keyList = new ArrayList<String>();

	@Override
	public Direction getSourceDirection() {
		return Direction.NONE;
	}

	@Override
	public void onStart(String url, HttpStatus status) {
		Response response = Response.current();
		response.contentType = "text/plain";

		response.status = status.getHttpCode();
		if(status.isFailed())
			this.failedStatus = status;
	}
	
	@Override
	public void complete(String url) {
		if (log.isDebugEnabled())
    		log.debug("finished streaming csv");
		writeFooter(null, null);
	}

	@Override
	public void addMissingData(String url, String errorMsg) {
		if (log.isWarnEnabled())
    		log.warn("Need to finish and add error at end of all streaming="+errorMsg);
	}

	@Override
	public void onFailure(String url, Throwable exception, String errorMsg) {
		writeFooter(exception, errorMsg);
		throw new SuccessfulAbort(errorMsg, exception);
	}

	private void writeFooter(Throwable error, String errorMsg) {
		//it's possible the connection has been closed, if so, give up
		try {
			Response response = Response.current();
			StringBuilder all = chunkToSend;
			if(failedStatus != null) {
				String msg = "";
				if(errorMsg != null)
					msg += errorMsg;
				if(error != null)
					msg += error;
				
				response.writeChunk(msg);
				return;
			}
			
			all.append(writeError(error, errorMsg));
			
			response.writeChunk(all);
		}
		catch (Exception e) {
			if (log.isInfoEnabled())
				log.info("can't write footer, connection is closed (or other error writing footer)", e);
		}
	}

	private String writeError(Throwable error, String errorMsg) {
		StringBuffer errorBlock = new StringBuffer();
		if(error != null || errorMsg != null) {
			errorBlock.append("\"error\":");
			errorBlock.append("\"");
		}
		
		if (error != null) {
			JsonStringEncoder inst = JsonStringEncoder.getInstance();
			errorBlock.append(inst.quoteAsString(""+error.getClass()+": "));
			errorBlock.append(inst.quoteAsString(""+error.getMessage()));
			String mode = Play.configuration.getProperty("application.mode");
			if(!"prod".equals(mode)) {
				inst = JsonStringEncoder.getInstance();
				errorBlock.append(inst.quoteAsString(""+ExceptionUtils.getStackTrace(error)));
			}

		}
		
		if(errorMsg != null) {
			errorBlock.append(errorMsg);
		}
		
		if(error != null || errorMsg != null)
			errorBlock.append("\"");
		
		return ""+errorBlock;
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		Response response = Response.current();
		rowCount++;

		translateToString(chunkToSend, row);

		if(rowCount >= chunkSize) {
			rowCount = 0;
			response.writeChunk(chunkToSend);
			chunkToSend = new StringBuilder();
		}
	}

	private void translateToString(StringBuilder chunkToSend2, TSRelational r) {
		if(isFirstRow) {
			formHeader(chunkToSend2, r);
			chunkToSend.append("\n");
			isFirstRow = false;
		}

		addRow(chunkToSend2, r);
		chunkToSend2.append("\n");
	}

	private void addRow(StringBuilder chunkToSend2, TSRelational r) {
		for(int i = 0; i < keyListSize; i++) {
			String k = keyList.get(i);
			Object val = r.get(k);
			chunkToSend2.append(val);
			if(i < keyListSize-1)
				chunkToSend2.append(",");
		}
	}

	private void formHeader(StringBuilder chunk, TSRelational r) {
		Set<String> keys = r.keySet();
		for(String key : keys) {
			keyList .add(key);
		}

		Collections.sort(keyList);
		
		for(int i = 0; i < keyList.size(); i++) {
			String k = keyList.get(i);
			chunk.append(k);
			if(i < keyList.size()-1)
				chunk.append(",");
		}
		keyListSize = keyList.size();
	}

	public void setCallbackParam(String callbackParam2) {
	}

	public void setChunkSize(int size) {
		this.chunkSize = size;
	}

}
