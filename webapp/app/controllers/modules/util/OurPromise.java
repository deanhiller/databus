package controllers.modules.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F;
import play.libs.F.Action;
import play.libs.F.Promise;

public class OurPromise<T> extends Promise<List<T>> {

	private static final Logger log = LoggerFactory.getLogger(OurPromise.class);
	public List<T> responses = new ArrayList<T>();
	public boolean suspended = false;
	
	//OKAY, This is complicated but basically, we ONLY want the callback to be called WHEN
	//1. responses.size > 0 (ie. we have responses)
	//2. We are waiting on the await method in the code
	//This guarantees then that we will not have the controller method being executed twice as previously
	//too many Runnables were being given to the thread pool and they step on each other.  There is no reason to
	//multi-thread a single request since downstream chunks you want in-order to begin with.
	
	public synchronized void addResponse(T obj) {
		//only invoke the invoke method once as we only need ONE runnable for
		//all the responses in the List...

		if(log.isDebugEnabled())
			log.debug("adding response");
		responses.add(obj);
		
        callCallbacks();
	}
	
	private void callCallbacks() {
		if(log.isDebugEnabled())
			log.debug("check on firing callback.  size="+responses.size()+" cbs="+callbacks+" suspended="+suspended);
		if(responses.size() <= 0 || !suspended) {
			return;			
		}
		
		if(log.isDebugEnabled())
			log.debug("firing all callbacks. callbacks="+callbacks);
		suspended = false; //we are unsuspending the controller until suspended again.
		for (F.Action<Promise<List<T>>> callback : callbacks) {
			if(log.isDebugEnabled())
				log.debug("firing callback");
			callback.invoke(this);
		}
	}

	public boolean isDone() {
		boolean isDone = responses.size() > 0;
		if(log.isDebugEnabled())
			log.debug("isdone="+isDone);
		return isDone;
	}

	
	@Override
	public synchronized List<T> get() throws InterruptedException, ExecutionException {	
		return null;
	}

	public synchronized List<T> resetAndGetResponses() {
		//reset so it can be re-used...
		if(log.isDebugEnabled())
			log.debug("resetting invoked and returning responses");
		super.invoked = false;
		List<T> temp = responses;
		responses = new ArrayList<T>();
		return temp;
	}
	
    public synchronized void onRedeem(F.Action<Promise<List<T>>> callback) {
    	suspended = true;
		if(log.isDebugEnabled())
			log.debug("adding callback="+callback);
    	if(callbacks.size() == 0)
    		callbacks.add(callback);
        	
    	callCallbacks();
    }
}
