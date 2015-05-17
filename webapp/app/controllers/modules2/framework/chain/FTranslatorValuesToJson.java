package controllers.modules2.framework.chain;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.http.HttpStatus;
import controllers.modules2.framework.procs.OutputProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

public class FTranslatorValuesToJson extends PushProcessorAbstract implements OutputProcessor {

	private static final Logger log = LoggerFactory.getLogger(FTranslatorValuesToJson.class);

	@Inject
	private ObjectMapper mapper;
	
	private String callbackParam;
	private boolean isFirstRow = true;
	private StringBuilder chunkToSend = new StringBuilder();
	private int chunkSize;
	private int rowCount = 0;
	private HttpStatus failedStatus;

	private List<String> errors = new ArrayList<String>();

	@Override
	public Direction getSourceDirection() {
		return Direction.NONE;
	}

	@Override
	public void onStart(String url, HttpStatus status) {
		Response response = Response.current();
		response.contentType = "application/json";
		String header = formHeader(response);

		response.status = status.getHttpCode();
		if(!status.isFailed()) {
			response.writeChunk(header);
		} else
			this.failedStatus = status;
	}

	private String formHeader(Response response) {
		String lastPart = "{\"data\":[";
		if (callbackParam != null) {
			response.contentType = "text/javascript";
			lastPart = callbackParam + "(" + lastPart;
		}
		return lastPart;
	}

	@Override
	public void complete(String url) {
		writeFooter(null, null);
	}
	
	
	@Override
	public void addMissingData(String url, String errorMsg) {
		if(errors.size() < 10) {
			errors.add(errorMsg);
		}
	}

	@Override
	public void onFailure(String url, Throwable exception, String errorMsg) {
		log.warn("Exception", exception);
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

			all.append("],");
			all.append(writeError(error, errorMsg));
			all.append("}");
			
			if (callbackParam != null) {
				all.append(");");
			}

			response.writeChunk(all);
		}
		catch (Exception e) {
			if (log.isInfoEnabled())
				log.info("can't write footer, connection is closed (or other error writing footer)", e);
		}
	}

	private String writeError(Throwable error, String errorMsg) {
		StringBuffer errorBlock = new StringBuffer("\"error\":");
		errorBlock.append("\"");
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

		for(int i = 0; i < errors.size(); i++) {
			String err = errors.get(i);
			errorBlock.append(err);
			if(i < errors.size())
				errorBlock.append(", ");
		}

		errorBlock.append("\"");
		return ""+errorBlock;
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		Response response = Response.current();
		rowCount++;
		if(isFirstRow) {
			isFirstRow = false;
		} else {
			chunkToSend.append(",");
		}		
		
		String rowStr = translateToString(row);
		chunkToSend.append(rowStr);

		if(rowCount >= chunkSize) {
			rowCount = 0;
			response.writeChunk(chunkToSend);
			chunkToSend = new StringBuilder();
		}
	}
	
	private String translateToString(TSRelational r) {
		try {
			return mapper.writeValueAsString(r);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setCallbackParam(String callbackParam2) {
		this.callbackParam = callbackParam2;
	}

	public void setChunkSize(int size) {
		this.chunkSize = size;
	}

}
