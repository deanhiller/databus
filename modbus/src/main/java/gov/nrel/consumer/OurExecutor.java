package gov.nrel.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OurExecutor {

	private static final Logger log = Logger.getLogger(OurExecutor.class.getName());
	private ThreadPoolExecutor svc;
	private ScheduledExecutorService scheduledSvc;
	private ThreadPoolExecutor recorderSvc;
	
	public OurExecutor(ScheduledExecutorService scheduledSvc, ExecutorService svc, ExecutorService recorderSvc) {
		this.scheduledSvc = scheduledSvc;
		this.svc = (ThreadPoolExecutor) svc;
		this.recorderSvc = (ThreadPoolExecutor)recorderSvc;
	}
	
	public void execute(Callable<Object> callable) {
		try {
			BlockingQueue<Runnable> queue = svc.getQueue();
			int size = queue.size();
			if(size > 200) {
				log.log(Level.WARNING, "Dropping task as queue is too full right now size="+size+" task="+callable, new RuntimeException("queue too full, system backing up"));
				return;
			}
			
			ExecutorRunnable r = new ExecutorRunnable(callable);
			svc.execute(r);
		} catch(Exception e) {
			log.log(Level.WARNING, "Exception moving task into thread pool", e);
		}
	}

	public ScheduledExecutorService getScheduledSvc() {
		return scheduledSvc;
	}

	public int getQueueCount() {
		BlockingQueue<Runnable> queue = svc.getQueue();
		return queue.size();
	}

	public int getActiveCount() {
		return svc.getActiveCount();
	}
	
	public ThreadPoolExecutor getRecorderService() {
		return recorderSvc;
	}
	
	public int getRecorderQueueCount() {
		BlockingQueue<Runnable> queue = recorderSvc.getQueue();
		return queue.size();
	}
}
