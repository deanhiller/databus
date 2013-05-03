package controllers.modules.util;

import java.util.ArrayList;
import java.util.List;

import com.ning.http.client.HttpResponseStatus;

public class Cache {

	private List<Object> responses = new ArrayList<Object>();
	private String failureBody ="";
	private HttpResponseStatus status;
	
	public void addResponse(Object r) {
		responses.add(r);
	}

	public void addToFailureBodyPart(String bodyPart) {
		this.failureBody += bodyPart;
	}

	public List<Object> getResponses() {
		return responses;
	}

	public String getFailureBody() {
		return failureBody;
	}

	public void setFailureBody(String failureBody) {
		this.failureBody = failureBody;
	}

	public void setStatus(HttpResponseStatus status) {
		this.status = status;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

}
