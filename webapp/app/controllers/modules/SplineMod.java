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
import play.mvc.Http.Response;

public class SplineMod extends Controller {

	public static final Logger log = LoggerFactory.getLogger(SplineMod.class);

	public static void error(String path) {
		badRequest("Your url is invalid as you need splineV1/{interval}/{windowSize}/{nextmodule}... and interval has to be long and windowSize has to be int");
	}
	
	public static void runSplines(long interval, int windowSize, String path)
			throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add(path);

		String param = request.params.get("callback");
		
		Processor p;
		if(request.isNew) { 
			int index = path.lastIndexOf("/");
			
			String endTime = path.substring(index+1);
			String leftOver = path.substring(0, index);
			int index2 = leftOver.lastIndexOf("/");
			String startTime = leftOver.substring(index2+1);

			if (log.isInfoEnabled())
				log.info("parsing path="+path+" ind="+index+" ind2="+index2+" leftOver="+leftOver+" starttime="+startTime);
			long start = Long.parseLong(startTime);
			long end = Long.parseLong(endTime);
			p = new SplineModProcessorImpl(interval, windowSize, start, end, param, response);
			request.args.put("processor", p);
		} else {
			p = (Processor) request.args.get("processor");
		}

		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while (latch.getCount() != 0) {
			if (log.isInfoEnabled())
				log.info("await for more stuff");
			await(promise);
			if (log.isInfoEnabled())
				log.info("firing into ModuleStreamer from splineMod");
			ModuleStreamer.processResponses(info, p);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls=" + urls + " total time="
				+ total + " ms");
	}


}
