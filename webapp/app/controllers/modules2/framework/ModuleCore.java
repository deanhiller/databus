package controllers.modules2.framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

import com.alvazan.play.NoSql;
import com.ning.http.client.ListenableFuture;

import controllers.modules2.framework.chain.DNegationProcessor;
import controllers.modules2.framework.chain.FTranslatorValuesToJson;
import controllers.modules2.framework.http.AbstractHttpMsg;
import controllers.modules2.framework.http.HttpBodyPart;
import controllers.modules2.framework.http.HttpCompleted;
import controllers.modules2.framework.http.HttpException;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.http.HttpStatus;
import controllers.modules2.framework.procs.DatabusBadRequest;
import controllers.modules2.framework.procs.OutputProcessor;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.EngineProcessor.TheRunnablePromise;
import controllers.modules2.framework.translate.TranslationFactory;

public class ModuleCore extends PlayPlugin {
//public class ModuleCore {

	private static final Logger log = LoggerFactory.getLogger(ModuleCore.class);

	@Inject
	private RawProcessorFactory procFactory;
	@Inject @Named("json")
	private TranslationFactory factory;
	@Inject
	private Provider<DNegationProcessor> negationProcessors;

	public PipelineInfo initialize(String path) {
		try {
			// OKAY, at this point, we are ready to rock and roll
			Request request = Request.current();
			PipelineInfo info;
			if (request.isNew) {
				info = createPipeline(path);
				request.args.put("info", info);
			} else {
				info = (PipelineInfo) request.args.get("info");
			}
	
			return info;
		} catch(DatabusBadRequest e) {
			log.info("user's bad request", e);
			throw new BadRequest(e.getMessage());
		}
	}

	private PipelineInfo createPipeline(String path) {
		PipelineInfo info;
		Request request1 = Request.current();
		String callbackParam = request1.params.get("callback");
		OurPromise<Object> promise = new OurPromise<Object>();

		//Here is where we define json/csv output
		String moduleName = "json";
		boolean hasOutputModule = false;
		if(path.startsWith("csv/")) {
			moduleName = "csv";
			hasOutputModule = true;
		} else if(path.startsWith("json/")) {
			hasOutputModule = true;
		}

		if(hasOutputModule)
			path = path.substring(moduleName.length()+1);
			
		RawProcessorFactory.threadLocal.set(moduleName);
		OutputProcessor toOutput = (OutputProcessor) procFactory.get();
		toOutput.setChunkSize(200);
		toOutput.setCallbackParam(callbackParam);
		ProcessorSetup lastOne = toOutput;

		boolean isReversed = false;
		String val = request1.params.get("reverse");
		if("true".equalsIgnoreCase(val)) {
			isReversed = true;
		}

		VisitorInfo visitor = new VisitorInfo(promise, factory, isReversed, NoSql.em());
		if(visitor.isReversed() && !StringUtils.contains(path, "dateformatV")) {
			lastOne = negationProcessors.get();
			Map<String, String> options = new HashMap<String, String>();
			lastOne.init("negation/"+path, toOutput, visitor, options);
		}

		try {
			lastOne.createPipeline(path, visitor, null, false);
			log.info("starting the pipeline="+lastOne);
			//start them all up now....
			lastOne.start(visitor);

			CountDownLatch latch = new CountDownLatch(visitor.getStreamCount());
			info = new PipelineInfo(visitor.getPromise(), latch, visitor.getRequestList(), toOutput, System.currentTimeMillis());
			return info;
		} catch(RuntimeException e) {
			if (log.isWarnEnabled())
        		log.warn("failure creating pipeline", e);
			cleanupSockets(visitor.getRequestList());
			throw e;
		}
	}

	public void processResponses(PipelineInfo info) {
		PushProcessor processor = info.getProcessor();
		try {
			if(info.isInFailure())
				return; //no need to do anything as code below already ran on first important failure...
			if(log.isDebugEnabled())
				log.debug("firing processing responses");
			processResponsesImpl(info);
		} catch(SuccessfulAbort e) { 
			//the last values almost always aborts after getting just enough data and uses SuccessfulAbort to do so
			if (log.isInfoEnabled())
				log.info("we successfully aborted and are closing the connection");
			cleanup(info);
		} catch (RuntimeException e) {
			if (log.isWarnEnabled())
        		log.warn("Exception occurred, about to cleanup", e);
			if (log.isInfoEnabled())
				log.info("about to cleanup");
			processor.onFailure(null, e, "Exception="+e.getMessage());
			info.setInFailure(true);
			cleanup(info);
			if (log.isInfoEnabled())
				log.info("cleaned up now so no more writes");
			throw e;
		} finally {
			CountDownLatch latch = info.getLatch();
			if(log.isDebugEnabled())
				log.debug("done firing processing. latch count=" + latch.getCount());
		}
	}

	private void processResponsesImpl(PipelineInfo info) {
		OurPromise<Object> promise2 = info.getPromise();
		List<Object> responses = promise2.resetAndGetResponses();
		for (Object resp : responses) {
			fireToListener(info, resp);
		}
	}

	private void fireToListener(PipelineInfo info, Object resp) {
		CountDownLatch latch = info.getLatch();
		if(resp instanceof TheRunnablePromise) {
			TheRunnablePromise p = (TheRunnablePromise) resp;
			p.run();
			return;
		} else if(!(resp instanceof AbstractHttpMsg)) {
			if (log.isWarnEnabled())
        		log.warn("resp type="+resp.getClass()+" not found");
			throw new RuntimeException("message type not defined here="+resp.getClass());
		}
		
		AbstractHttpMsg msg = (AbstractHttpMsg) resp;
		HttpListener listener = msg.getListener();
		if(msg instanceof HttpStatus) {
			listener.onStatus((HttpStatus) msg);
		} else if(msg instanceof HttpBodyPart) {
			listener.onBodyPartReceived((HttpBodyPart) msg);
		} else if(msg instanceof HttpCompleted) {
			listener.onCompleted((HttpCompleted) msg);
			latch.countDown();
		} else if(msg instanceof HttpException) {
			listener.onThrowable((HttpException)msg);
		} else {
			if (log.isWarnEnabled())
        		log.warn("resp type="+resp.getClass()+" not found");
			throw new RuntimeException("what is this="+resp);
		}
	}

	private void cleanup(PipelineInfo info) {
		exhaustTheLatch(info.getLatch());
		cleanupSockets(info.getRequestList());
	}

	private void cleanupSockets(List<ListenableFuture<Object>> connections) {
		for(ListenableFuture<Object> connection : connections) {
			try {
				connection.abort(null);
			} catch(Exception e) {
				if (log.isWarnEnabled())
	        		log.warn("trouble closing connection", e);
			}
		}
	}
	
	private void exhaustTheLatch(CountDownLatch latch) {
		while (latch.getCount() != 0) {
			latch.countDown();
		}
	}

}
