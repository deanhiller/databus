package controllers.modules2;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import controllers.modules2.framework.ModuleCore;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.ProductionModule;

import play.mvc.Http.Request;
import play.mvc.Scope;

public class NewFrameworkTest {

	//@Test
	public void testBasicUrl() {
		String url = "splineV1Beta/60/invertV1Beta/demux/nameOfAggregation/0/555555";
		Request r = new Request();
		Request.current.set(r);
		ModuleCore setup = ModuleController.createCore();
		setup.initialize(url);
		
	}
}
