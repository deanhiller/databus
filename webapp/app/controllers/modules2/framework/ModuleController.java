package controllers.modules2.framework;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http.Request;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ModuleController extends Controller {

	private static final Logger log = LoggerFactory.getLogger(ModuleController.class);
	
	private static ModuleCore core;

	private static RawProcessorFactory theFactory;
	
	static {
		core = createCore();
	}

	public static ModuleCore createCore() {
		RawProcessorFactory factory = new RawProcessorFactory();
		ProductionModule productionModule = new ProductionModule(factory);
		Injector injector = Guice.createInjector(productionModule);
		ModuleCore core1 = injector.getInstance(ModuleCore.class);
		factory.setInjector(injector);
		theFactory = factory;
		return core1;
	}
	
	public static RawProcessorFactory fetchFactory() {
		return theFactory;
	}

	public static ModuleCore fetchCore() {
		String mode = Play.configuration.getProperty("application.mode");
		if("dev".equals(mode)) {
			core = createCore();
		}
		return core;
	}
	
	public static void startModules(String path) {
		//Because of the on-demand development recompile, dev mode has to recreate the core or changes won't be there
		String mode = Play.configuration.getProperty("application.mode");
		if("dev".equals(mode)) {
			core = createCore();
		}
		
		PipelineInfo info = core.initialize(path);
		
		OurPromise<Object> promise = info.getPromise();
		CountDownLatch latch = info.getLatch();
		
		while (latch.getCount() != 0  && !info.isInFailure()) {
			if (log.isDebugEnabled())
				log.debug("await for more stuff="+path);
			await(promise);
			if (log.isDebugEnabled())
				log.debug("firing into ModuleStreamer from splineMod="+path);
			core.processResponses(info);
		}
		
		Request req = Request.current();
		req.args.put("__lastCall", true);
	}
}
