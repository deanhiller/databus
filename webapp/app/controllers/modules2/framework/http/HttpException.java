package controllers.modules2.framework.http;

public class HttpException extends AbstractHttpMsg {

	private Throwable exception;

	public HttpException(String url, HttpListener listener, Throwable ex) {
		super(url, listener);
		this.exception = ex;
	}

	public Throwable getException() {
		return exception;
	}
	
}
