package controllers.modules2.framework.chain;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

import controllers.modules2.framework.OurPromise;
import controllers.modules2.framework.http.HttpBodyPart;
import controllers.modules2.framework.http.HttpCompleted;
import controllers.modules2.framework.http.HttpException;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.http.HttpStatus;

public class AHttpChunkingListener implements AsyncHandler<Object> {

	private static final Logger log = LoggerFactory.getLogger(AHttpChunkingListener.class);
	
	private HttpListener listener;
	private OurPromise<Object> promise;
	private String url;

	@Override
	public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart part) throws Exception {
		try {
			ByteBuffer buffer = part.getBodyByteBuffer();
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);

			promise.addResponse(new HttpBodyPart(data, url, listener));
			
			return STATE.CONTINUE;
		} catch(Exception e) {
			onThrowable(e);
			return STATE.ABORT;
		}
	}

	@Override
	public Object onCompleted() throws Exception {
		try {
			promise.addResponse(new HttpCompleted(url, listener));
			return null;
		} catch(Exception e) {
			onThrowable(e);
			return null;
		}
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(
			HttpResponseHeaders headers) throws Exception {
		log.info("headers="+headers);
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onStatusReceived(HttpResponseStatus status) throws Exception {
		try {
			if (log.isDebugEnabled())
				log.debug("status received="+status+" cd="+status.getStatusCode()+" txt="+status.getStatusText());
			promise.addResponse(new HttpStatus(url, listener, status));
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
		promise.addResponse(new HttpException(url, listener, ex));
	}

	public void setPromise(OurPromise<Object> promise2) {
		this.promise = promise2;
	}

	public HttpListener getRealListener() {
		return listener;
	}

	public void setUrl(String url2) {
		this.url = url2;
	}

	public void setListener(HttpListener listener2) {
		this.listener = listener2;
	}
}