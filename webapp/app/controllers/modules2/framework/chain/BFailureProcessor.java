package controllers.modules2.framework.chain;

import java.nio.charset.Charset;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Response;
import controllers.modules2.framework.Config;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.http.HttpBodyPart;
import controllers.modules2.framework.http.HttpCompleted;
import controllers.modules2.framework.http.HttpException;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.http.HttpStatus;
import controllers.modules2.framework.procs.PushProcessor;

public abstract class BFailureProcessor implements HttpListener {

	@Inject
	private Config config;
	
	private static final Logger log = LoggerFactory.getLogger(CTranslatorJsonToValues.class);
	protected PushProcessor processor;
	protected boolean started = false;
	private HttpStatus status;
	private String bodyForFailure = "";
	
	@Override
	public void setProcessor(PushProcessor processor) {
		if (log.isDebugEnabled())
			log.debug("set processor");
		this.processor = processor;
	}

	@Override
	public void onStatus(HttpStatus status) {
		if (log.isDebugEnabled())
			log.debug("on status");
		if(!started) {
			this.status = status;
			String url = status.getUrl();
			//start the stream on success AND on failure
			processor.onStart(url, status);
			started = true;
		}
	}

	@Override
	public void onBodyPartReceived(HttpBodyPart part) {
		try {
			onBodyPartReceivedImpl(part);
		} catch(SuccessfulAbort e) {
			processor.complete(part.getUrl());
			throw e;
		} catch(RuntimeException e) {
			processor.onFailure(part.getUrl(), e, "Exception processing body");
			throw e;
		}
	}
	public void onBodyPartReceivedImpl(HttpBodyPart part) {
		byte[] data = part.getData();
		if(status.isFailed()) {
			//our failures typically have a String as the body coming back...
			String s = new String(data, Charset.forName("ISO-8859-1"));
			bodyForFailure += s;
			return;
		}
		
		//NOW, we are still not out of the woods yet.  We need to see if this is a special
		//failure chunk because if we pass it to the translator, he probably can't translate it
		//since the error chunk is different????
		String url = part.getUrl();
		onBytesReceived(url, data);
	}

	protected abstract void onBytesReceived(String url, byte[] data);

	@Override
	public void onCompleted(HttpCompleted completed) {
		if (log.isDebugEnabled())
			log.debug("on complete");
		
		if(status.isFailed()) {
			fireFailureBody();
			return;
		}
		
		String url = completed.getUrl();
		processor.complete(url);
	}

	private void fireFailureBody() {
		String url = status.getUrl();
		String fullUrl = config.getBaseUrl()+url;
		int code = status.getHttpCode();
		String statusText = status.getHttpMessage(); 
		String msg = "Failure when getting url=" + fullUrl
				+ " statusCode=" + code + " statusTxt=" + statusText
				+ " BODY=(" + bodyForFailure+")";

		if (log.isDebugEnabled())
			log.debug(msg);
		
		processor.onFailure(url, null, msg);
	}

	@Override
	public void onThrowable(HttpException httpExc) {
		String url = httpExc.getUrl();
		Throwable exc = httpExc.getException();
		String msg = exc.getMessage();
		log.info("onthrowable. started="+started);
		if(!started) {
			started = true;
			this.status = new HttpStatus(url, 500, "Failure="+msg);
			processor.onStart(url, status);
		}
		log.info("running failure code. started="+started+" status="+status);
		if(status.isFailed())
			fireFailureBody();
		else {
			if (log.isInfoEnabled())
				log.info("on throwable");
			processor.onFailure(url, exc, msg);
		}
	}
}
