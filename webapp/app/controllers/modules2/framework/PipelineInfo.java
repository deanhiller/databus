package controllers.modules2.framework;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.ning.http.client.ListenableFuture;

import controllers.modules2.framework.procs.PushProcessor;

public class PipelineInfo {

	private OurPromise<Object> promise;
	private CountDownLatch latch;
	private List<ListenableFuture<Object>> requestList;
	private boolean isInFailure = false;
	private PushProcessor processor;
	private long startTime;
	
	public PipelineInfo(OurPromise<Object> promise, CountDownLatch latch2, List<ListenableFuture<Object>> list, PushProcessor lastInChain, long startTime) {
		this.promise = promise;
		this.latch = latch2;
		this.requestList = list;
		this.processor = lastInChain;
		this.startTime = startTime;
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

	public PushProcessor getProcessor() {
		return processor;
	}

	public long getStartTime() {
		return startTime;
	}
}
