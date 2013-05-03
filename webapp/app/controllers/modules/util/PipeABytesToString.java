package controllers.modules.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.AsyncHandler.STATE;


class PipeABytesToString implements AsyncHandler<Object> {

	private static final Logger log = LoggerFactory.getLogger(PipeABytesToString.class);
	private PipeBStringToJson stringHandler;
	private String url;
	private boolean failureMode;
	private PipeCPromiseWriter promiseWriter;
	
	public PipeABytesToString(PipeCPromiseWriter promiseWriter, String url) {
		this.promiseWriter = promiseWriter;
		this.stringHandler = new PipeBStringToJson(promiseWriter, url);
		this.url = url;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(
			HttpResponseBodyPart part) throws Exception {
		try {
			ByteBuffer buffer = part.getBodyByteBuffer();
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			
			String s = new String(data, Charset.forName("ISO-8859-1"));
			if(failureMode)
				promiseWriter.onFailureBodyPart(url, "[DOWNSTREAM FAILURE="+s+"]");
			else
				stringHandler.onBodyPartReceived(s);
			
			return STATE.CONTINUE;
		} catch(Exception e) {
			onThrowable(e);
			return STATE.ABORT;
		}
	}

	@Override
	public Object onCompleted() throws Exception {
		try {
			return stringHandler.onCompleted(url);
		} catch(Exception e) {
			onThrowable(e);
			return null;
		}
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(
			HttpResponseHeaders headers) throws Exception {
		if (log.isInfoEnabled())
			log.info("headers="+headers);
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onStatusReceived(
			HttpResponseStatus status) throws Exception {
		try {
			if (log.isInfoEnabled())
				log.info("status received="+status+" cd="+status.getStatusCode()+" txt="+status.getStatusText());
			if(status.getStatusCode() != 200)
				failureMode = true;
			stringHandler.onStatus(url, status);
			return STATE.CONTINUE;
		} catch(Exception e) {
			onThrowable(e);
			return STATE.ABORT;
		}
	}

	@Override
	public void onThrowable(Throwable ex) {
		if(ex == null)
			return;
		
		if (log.isWarnEnabled())
    		log.warn("Exception", ex);
		stringHandler.onThrowable(url, ex);
	}
}