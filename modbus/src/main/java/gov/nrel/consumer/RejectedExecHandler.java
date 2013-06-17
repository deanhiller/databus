package gov.nrel.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RejectedExecHandler implements RejectedExecutionHandler {

	private static final Logger log = Logger.getLogger(RejectedExecHandler.class.getName());
	
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		BlockingQueue<Runnable> queue = executor.getQueue();
		
		long time = System.currentTimeMillis();
		try {
			queue.put(r);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		long total = System.currentTimeMillis() - time;
		
		log.log(Level.WARNING, "Downstream not keeping up.  We waited to add to queue time="+total, new RuntimeException("time="+total));
	}

}
