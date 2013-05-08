package controllers.modules2.framework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import com.google.inject.Injector;

import controllers.modules2.FirstValuesProcessor;
import controllers.modules2.InvertProcessor;
import controllers.modules2.LogProcessor;
import controllers.modules2.MinMaxProcessor;
import controllers.modules2.MultiplyProcessor;
import controllers.modules2.PassthroughProcessor;
import controllers.modules2.RangeCleanProcessor;
import controllers.modules2.RawProcessor;
import controllers.modules2.SplinesPullProcessor;
import controllers.modules2.SqlPullProcessor;
import controllers.modules2.SumStreamProcessor;
import controllers.modules2.TimeAverageProcessor;
import controllers.modules2.framework.chain.FTranslatorValuesToCsv;
import controllers.modules2.framework.chain.FTranslatorValuesToJson;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.RemoteProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

public class RawProcessorFactory implements Provider<ProcessorSetup> {
	
	public static final ThreadLocal<String> threadLocal = new ThreadLocal<String>();
	
	private Map<String, Class<?>> nameToClazz = new HashMap<String, Class<?>>();
	private Set<String> moduleNamesToForward = new HashSet<String>();
	private Injector injector;

	public RawProcessorFactory() {
		nameToClazz.put("getdataV1", SqlPullProcessor.class);
		nameToClazz.put("logV1", LogProcessor.class);
		nameToClazz.put("splinesV2", SplinesPullProcessor.class);
		nameToClazz.put("rangecleanV1", RangeCleanProcessor.class);
		nameToClazz.put("timeaverageV2", TimeAverageProcessor.class);
		nameToClazz.put("invertV1", InvertProcessor.class);
		nameToClazz.put("passthroughV1", PassthroughProcessor.class);
		nameToClazz.put("rawdataV1", RawProcessor.class);
		nameToClazz.put("demux", StreamsProcessor.class);
		nameToClazz.put("firstvaluesV1", FirstValuesProcessor.class);
		nameToClazz.put("minmaxV1", MinMaxProcessor.class);
		nameToClazz.put("multiplyV1", MultiplyProcessor.class);
		nameToClazz.put("sumstreamsV1", SumStreamProcessor.class);
		nameToClazz.put("csv", FTranslatorValuesToCsv.class);
		nameToClazz.put("json", FTranslatorValuesToJson.class);

		//moduleNamesToForward.add("passthroughV1");
		//moduleNamesToForward.add("invertV1");
		//moduleNamesToForward.add("cacheV1");
		//moduleNamesToForward.add("rangecleanV1");
		//moduleNamesToForward.add("sumstreamsV1");
		//moduleNamesToForward.add("getdataV1");
		moduleNamesToForward.add("stddevV1");
		moduleNamesToForward.add("timeaverageV1"); //ported as timeaverageV2
		moduleNamesToForward.add("splineV1");      //ported as splineV2
		moduleNamesToForward.add("splinesV1");     //ported as splineV2
		moduleNamesToForward.add("lastvaluesV1");  //ported as firstvaluesV1 with reverse=true parameter
		moduleNamesToForward.add("nullV1");  //not being ported
		moduleNamesToForward.add("lastvaluesV1Beta"); //not being ported
		moduleNamesToForward.add("rawdataGeneratorV1"); //not being ported
	}
	
	@Override
	public ProcessorSetup get() {
		String moduleName = threadLocal.get();
		if(moduleName == null)
			throw new IllegalArgumentException("Please call ProcessorFactory.threadLocal.set(moduleName)");
		Class<?> clazz = nameToClazz.get(moduleName);
		if(clazz == null) {
			if(moduleNamesToForward.contains(moduleName))
				return injector.getInstance(RemoteProcessor.class);
			return null;
		}
		
		try {
			Object newInstance = injector.getInstance(clazz);
			
			//Object newInstance = clazz.newInstance();
			return (ProcessorSetup) newInstance;
//		} catch (InstantiationException e) {
//			throw new RuntimeException(e);
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e);
		} finally {
			threadLocal.set(null);
		}
	}

	public void setInjector(Injector injector) {
		this.injector = injector;
	}
	
}
