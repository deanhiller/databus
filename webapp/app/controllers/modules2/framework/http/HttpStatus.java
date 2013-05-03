package controllers.modules2.framework.http;

import com.ning.http.client.HttpResponseStatus;

public class HttpStatus extends AbstractHttpMsg {

	private int httpCode;
	private String httpMessage;

	public HttpStatus(String url, int httpCode, String msg) {
		super(url, null);
		this.httpCode = httpCode;
		this.httpMessage = msg;
	}
	
	public HttpStatus(String url, HttpListener listener, HttpResponseStatus status) {
		super(url, listener);
		httpCode = status.getStatusCode();
		httpMessage = status.getStatusText();
	}

	public boolean isFailed() {
		if(httpCode != 200)
			return true;
		return false;
	}

	public int getHttpCode() {
		return httpCode;
	}

	public String getHttpMessage() {
		return httpMessage;
	}
	
}
