package gov.nrel.consumer;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutorRunnable implements Runnable {

	private static final Logger log = Logger.getLogger(ExecutorRunnable.class.getName());
	private Callable<Object> callable;

	public ExecutorRunnable(Callable<Object> callable) {
		this.callable = callable;
	}

	@Override
	public void run() {
		try {
			callable.call();
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception", e);
		}
	}
	
}
