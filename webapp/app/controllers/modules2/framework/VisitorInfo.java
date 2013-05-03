package controllers.modules2.framework;

import java.util.ArrayList;
import java.util.List;

import com.ning.http.client.ListenableFuture;

import controllers.modules2.framework.translate.TranslationFactory;

public class VisitorInfo {

	private List<ListenableFuture<Object>> requestList = new ArrayList<ListenableFuture<Object>>();
	private OurPromise<Object> promise;
	private int streamCount = 0;
	private TranslationFactory translator;
	private boolean isReversed; 
	
	public VisitorInfo(OurPromise<Object> promise, TranslationFactory translator, boolean isReversed) {
		this.promise = promise;
		this.translator = translator;
		this.isReversed = isReversed;
	}

	public List<ListenableFuture<Object>> getRequestList() {
		return requestList;
	}

	public OurPromise<Object> getPromise() {
		return promise;
	}

	public int getStreamCount() {
		return streamCount;
	}

	public TranslationFactory getTranslator() {
		return translator;
	}

	public void incrementStreamCount() {
		streamCount++;
	}

	public boolean isReversed() {
		return isReversed;
	}
	
	
}
