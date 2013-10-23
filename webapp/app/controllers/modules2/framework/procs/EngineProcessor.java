package controllers.modules2.framework.procs;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.util.ExecutorsSingleton;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.OurPromise;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.http.HttpBodyPart;
import controllers.modules2.framework.http.HttpCompleted;
import controllers.modules2.framework.http.HttpException;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.http.HttpStatus;


public class EngineProcessor extends ProcessorSetupAbstract {

	private static final Logger log = LoggerFactory.getLogger(EngineProcessor.class);
	private static final EmptyListener empty = new EmptyListener();
	private TheRunnablePromise ourRunnable;
	private boolean isRunning = false;
	private OurPromise<Object> promise;
	private AddToPromiseRunnable addToPromise = new AddToPromiseRunnable();
	
	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		super.init(path, nextInChain, visitor, options);
		//NOTE: only different from super.init is we return the same path that is passed in to us...
		return path;
	}

	@Override
	public void start(VisitorInfo visitor) {
		visitor.incrementStreamCount();

		child.start(visitor);
		promise = visitor.getPromise();
		ourRunnable = new TheRunnablePromise((PullProcessor) child, (PushProcessor) parent, promise);
		promise.addResponse(ourRunnable);
		isRunning = true;
	}

	public class TheRunnablePromise implements Runnable {

		private PullProcessor source;
		private PushProcessor nextInChain;
		private OurPromise<Object> promise;
		private RunFlag flag;
		private boolean isStarted = false;
		private boolean firstRowFailure = false;
		
		public TheRunnablePromise(PullProcessor source2,
				PushProcessor nextInChain2, OurPromise<Object> promise) {
			this.source = source2;
			this.nextInChain = nextInChain2;
			this.promise = promise;
			flag = new RunFlag(this, promise);
		}

		@Override
		public void run() {
			String url = source.getUrl();
			try {
				runReadLoop(url);
			} catch(SuccessfulAbort e) {
				if (log.isInfoEnabled())
					log.info(EngineProcessor.this+"in engine successful abort");
				nextInChain.complete(url);
				promise.addResponse(new HttpCompleted(url, empty));
			} catch(Exception e) {
				if (log.isWarnEnabled())
	        		log.warn(EngineProcessor.this+"exception", e);
				if(firstRowFailure) {
					if (log.isInfoEnabled())
						log.info(EngineProcessor.this+"this is a first row failure so send http status");
					HttpStatus status = new HttpStatus(url, 500, null);
					nextInChain.onStart(url, status);
				}
				nextInChain.onFailure(url, e, "Failed to read and write to next in chain="+nextInChain);
			}
		}

		private void runReadLoop(String url) throws Exception {
			for(int i = 0; i < 200; i++) {
				ReadResult result = null;
				if(!isStarted) {
					result = tryRead(url);
				} else 
					result = source.read();

				if(result == null) {//source has no more data in their buffers yet so return for now
					log.info(EngineProcessor.this+"isRunning is set to false");
					isRunning = false;
					return;
				} else if(result.isEndOfStream()) {
					if (log.isInfoEnabled())
						log.info(EngineProcessor.this+"aborting stream since it is the end of stream");
					throw new SuccessfulAbort("we are done reading the stream");
				} else {
					processRow(result, url);
				}
			}

			if(flag.isSocketReadOn()) {
				ExecutorsSingleton.executor.execute(addToPromise);
			} else {
				if (log.isInfoEnabled())
					log.info(EngineProcessor.this+"don't add promise since flag isSocketRead was set to false.  ie. pause engine");
				isRunning = false;
			}
		}

		private ReadResult tryRead(String url) throws Exception {
			try {
				isStarted = true;
				ReadResult result = source.read();
				HttpStatus status = new HttpStatus(url, 200, null);
				nextInChain.onStart(url, status);
				return result;
			} catch(Exception e) {
				if (log.isInfoEnabled())
					log.info(EngineProcessor.this+"first row failure", e);
				firstRowFailure = true;
				throw e;
			}
		}

		private void processRow(ReadResult result, String url) {
			if(result.getErrorMsg() != null) {
				if (log.isWarnEnabled())
	        		log.warn(EngineProcessor.this+"seems we have missing data="+result.getRow());
				nextInChain.addMissingData(url, result.getErrorMsg());
			} else {
				TSRelational row = result.getRow();
				if(row != null)
					nextInChain.incomingChunk(url, row, flag);
			}
		}
	}

	private class AddToPromiseRunnable implements Runnable {
		@Override
		public void run() {
			promise.addResponse(ourRunnable);
		}
	}
	
	private class RunFlag implements ProcessedFlag {
		private boolean socketReadOn = true;
		private TheRunnablePromise runnable;
		private OurPromise<Object> promise;

		public RunFlag(TheRunnablePromise theRunnablePromise, OurPromise<Object> promise) {
			runnable = theRunnablePromise;
			this.promise = promise;
		}

		@Override
		public void setSocketRead(boolean on) {
			if (log.isInfoEnabled())
				log.info(EngineProcessor.this+"someone is setting socketread="+on);
			if(!socketReadOn && on) {
				promise.addResponse(runnable);
			}
			this.socketReadOn = on;
		}

		public boolean isSocketReadOn() {
			return socketReadOn;
		}
	}
	
	private static class EmptyListener implements HttpListener {
		@Override
		public void setProcessor(PushProcessor processor) {
		}

		@Override
		public void onStatus(HttpStatus status) {
		}

		@Override
		public void onBodyPartReceived(HttpBodyPart part) {
		}

		@Override
		public void onCompleted(HttpCompleted completed) {
		}

		@Override
		public void onThrowable(HttpException httpExc) {
		}
	}

	@Override
	public Direction getSinkDirection() {
		return Direction.PULL;
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.PUSH;
	}

	public void startEngine() {
		if(isRunning)
			return;
		if (log.isInfoEnabled())
			log.info(this+"kicking the engine that is not running");
		isRunning = true;
		promise.addResponse(ourRunnable);
	}

}
