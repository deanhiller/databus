package controllers.modules.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.HttpResponseStatus;

/**
 * Caches tons of stuff until all status codes are in at which point it sends in all the responses in cache to the
 * request thread to be processed.
 * 
 * @author dhiller2
 */
class PipeCPromiseWriter {

	private static final Logger log = LoggerFactory.getLogger(PipeCPromiseWriter.class);
	
	private OurPromise<Object> promise;
	private Map<String, Cache> cache = new HashMap<String, Cache>();
	private int numUrlsWaitingOnStatus;
	private String failureUrl = null;
	
	public PipeCPromiseWriter(OurPromise<Object> promise, List<String> urls) {
		this.promise = promise;
		for(String url : urls) {
			if (log.isInfoEnabled())
				log.info("creating cache for url="+url);
			cache.put(url, new Cache());
		}
		this.numUrlsWaitingOnStatus = urls.size();
	}

	public void complete(String url) {
		synchronized(promise) {
			if (log.isInfoEnabled())
				log.info("complete.  failure url="+failureUrl+" numUrlsWaiting="+numUrlsWaitingOnStatus);
			if(isFailed()) {
				if(failureUrl.equals(url))
					sendFailure(url);
				return;
			}
			
			Complete c = new Complete();
			c.setUrl(url);
			if(numUrlsWaitingOnStatus > 0) {
				if (log.isInfoEnabled())
					log.info("cache the complete response");
				Cache cache2 = cache.get(url);
				cache2.addResponse(c);
				return;
			}

			if (log.isInfoEnabled())
				log.info("add completion to promise");
			promise.addResponse(c);
		}
	}

	private void sendFailure(String url) {
		Cache cache2 = cache.get(url);
		String body = cache2.getFailureBody();
		HttpResponseStatus status = cache2.getStatus();
		HttpBadResponse resp = new HttpBadResponse(url, status, body);
		promise.addResponse(resp);
		cache.clear();
	}

	public void incomingJsonRow(JsonRow r) {
		synchronized (promise) {
			if(isFailed())
				return;
			else if(numUrlsWaitingOnStatus > 0) {
				Cache cache2 = cache.get(r.getUrl());
				cache2.addResponse(r);
				return;
			}
			promise.addResponse(r);
		}
	}

	public void onThrowable(String url2, Throwable ex) {
		//don't log, it is logged elsewhere
		if (log.isInfoEnabled())
			log.info("on throwable");
		Problem p = new Problem();
		p.setException(ex);
		p.setUrl(url2);
		promise.addResponse(p);
	}

	public boolean isFailed() {
		return failureUrl != null;
	}
	
	public void onStatus(String url, HttpResponseStatus status) {
		synchronized(promise) {
			if (log.isInfoEnabled())
				log.info("onStatus.  cd="+status.getStatusCode()+" url="+url+" numwaitingOnStatus="+numUrlsWaitingOnStatus);
			if(status.getStatusCode() != 200) {
				failureUrl = url;
				if (log.isInfoEnabled())
					log.info("url="+url);
				Cache cache2 = cache.get(url);
				cache2.setStatus(status);
				//we return so that we don't call completeStart ever as we are going to send
				//back a failure to the client
				return;
			}

			numUrlsWaitingOnStatus--;
			if(numUrlsWaitingOnStatus == 0)
				completeStart();
		}
	}

	private void completeStart() {
		if (log.isInfoEnabled())
			log.info("complete start");
		//okay, all streams are ready so send the start message
		promise.addResponse("start");
		
		//completely flush the cache now that every stream is started...
		for(Cache cache2 : cache.values()) {
			for(Object obj : cache2.getResponses()) {
				promise.addResponse(obj);
			}
		}
		cache.clear();
		cache = null;
	}

	public void onFailureBodyPart(String url, String bodyPart) {
		synchronized (promise) {
			Cache cache2 = cache.get(url);
			cache2.addToFailureBodyPart(bodyPart);
		}
	}
}