package controllers.modules2.framework.http;

public class AbstractHttpMsg {

	private HttpListener listener;
	private String url;

	public AbstractHttpMsg(String url, HttpListener listener) {
		this.url = url;
		this.listener = listener;
	}

	public HttpListener getListener() {
		return listener;
	}

	public String getUrl() {
		return url;
	}
	
}
