package controllers.modules.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.ning.http.client.ListenableFuture;

public class Info {

	private OurPromise<Object> promise;
	private CountDownLatch latch;
	private List<ListenableFuture<Object>> requestList;
	private boolean isInFailure = false;
	
	public Info(OurPromise<Object> promise, CountDownLatch latch, List<ListenableFuture<Object>> requestList) {
		this.promise = promise;
		this.latch = latch;
		this.requestList = requestList;
	}

	public OurPromise<Object> getPromise() {
		return promise;
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public List<ListenableFuture<Object>> getRequestList() {
		return requestList;
	}

	public boolean isInFailure() {
		return isInFailure;
	}

	public void setInFailure(boolean isInFailure) {
		this.isInFailure = isInFailure;
	}
	
}
