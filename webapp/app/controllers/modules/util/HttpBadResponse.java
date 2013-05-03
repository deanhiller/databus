package controllers.modules.util;

import com.ning.http.client.HttpResponseStatus;

public class HttpBadResponse {

	private String url;
	private HttpResponseStatus status;
	private String body;

	public HttpBadResponse(String url, HttpResponseStatus status, String body) {
		this.url = url;
		this.status = status;
		this.body = body;
	}

	public String getUrl() {
		return url;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public String getBody() {
		return body;
	}

}
