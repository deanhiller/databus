package controllers.gui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorsSingleton {

	public final static Executor executor;
	
	static {
		//LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(1000);
		//RejectedExecutionHandler rejectedExec = new RejectedExecHandler();
		//executor = new ThreadPoolExecutor(20, 20, 120, TimeUnit.SECONDS, queue, rejectedExec );
		executor = Executors.newFixedThreadPool(20);
	}
	
}
