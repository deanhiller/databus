package controllers.modules;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules.util.Info;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Util;

public class WindowOperationMod extends Controller {

	public static final Logger log = LoggerFactory.getLogger(CleanerMod.class);

	public static void error(String path) {
		badRequest("Your url is invalid as you need splinesV1/{splineType}/{interval}/{nextmodule}... and splineType must be basic or limitderivative");
	}
	
	public static void error2(String path) {
		badRequest("Your url is invalid as you need splineV1/{interval}/{windowsize}/{nextmodule}... and widowsize is ignored (hard coded to 4)");
	}
	
	public static void runSplinesDepricated(long interval, long windowSize, String path) throws InterruptedException, ExecutionException, IOException {
		runSplinesImpl("basic", interval, path);
	}
	
	public static void runSplines(String splineType, long interval, String path)
			throws InterruptedException, ExecutionException, IOException {
		runSplinesImpl(splineType, interval, path);
	}
	
	@Util
	public static void runSplinesImpl(String types, long interval, String path)
			throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);
		
		String param = request.params.get("callback");
		boolean reverse = false;
		String paramReverse2 = request.params.get("reverse");
		if(paramReverse2.equalsIgnoreCase("true"))
			reverse = true;
		
		//if (log.isInfoEnabled())
			//log.info("paramReverse: {}",paramReverse);
		
		Processor p;
		if(request.isNew) { 
			PathParse pathParse = new PathParse();
			long[] startEnd = pathParse.calculateStartEnd(path);

			if(reverse) {
				p = new WindowOperationModProcessor(interval, startEnd[0], startEnd[1], param);
			} else {
				p = new WindowOperationModProcessor(interval, startEnd[1], startEnd[0], param);
			}
			request.args.put("processor", p);
		} else {
			p = (Processor) request.args.get("processor");
		}

		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while (latch.getCount() != 0  && !info.isInFailure()) {
			if (log.isDebugEnabled())
				log.debug("await for more stuff");
			await(promise);
			if (log.isDebugEnabled())
				log.debug("firing into ModuleStreamer from splineMod");
			ModuleStreamer.processResponses(info, p);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}


}
