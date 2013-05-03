package controllers.modules;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import controllers.modules.util.Info;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;

public class PassthroughMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(PassthroughMod.class);

	public static void fetchData() throws InterruptedException, ExecutionException, IOException {
		List<String> urls = new ArrayList<String>();
		urls.add("http://localhost:9000/rawdata/fakeTimeSeries/0/123423432423423423");
		
		String param = request.params.get("callback");
		
		PassthroughProcessor processor = new PassthroughProcessor(param);
		Info info = ModuleStreamer.startStream(urls);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while(latch.getCount() != 0) {
			await(promise);
			ModuleStreamer.processResponses(info, processor);
		}

		Long start = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - start;
		if (log.isInfoEnabled())
			log.info("finished with request to urls="+urls+" total time="+total+" ms");
	}
}
