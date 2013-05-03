package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import controllers.modules.mocks.MockProcessor;
import controllers.modules2.EmptyFlag;
import controllers.modules2.MinMaxProcessor;
import controllers.modules2.framework.ModuleCore;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ProductionModule;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.DNegationProcessor;
import controllers.modules2.framework.chain.FTranslatorValuesToJson;
import controllers.modules2.framework.procs.PushProcessor;

public class TestModuleHookup {

	//@Test
	public void testMinMax() {

		RawProcessorFactory factory = new RawProcessorFactory();
		ProductionModule productionModule = new ProductionModule(factory);
		Injector injector = Guice.createInjector(productionModule);
		factory.setInjector(injector);

		VisitorInfo visitor = new VisitorInfo(null, null, false);
		FTranslatorValuesToJson proc2 = injector.getInstance(FTranslatorValuesToJson.class);
		proc2.createPipeline("doubleV1/invertV1Beta/rawdataInnerV1/name/50/2000", visitor, null, false);
		
		
		FTranslatorValuesToJson proc = injector.getInstance(FTranslatorValuesToJson.class);
		proc.createPipeline("invertV1Beta/doubleV1/rawdataInnerV1/name/50/2000", visitor, null, false);
		

	}

}
