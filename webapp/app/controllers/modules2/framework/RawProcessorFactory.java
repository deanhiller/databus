package controllers.modules2.framework;

import com.google.inject.Injector;
import controllers.modules2.AggregationPipeProcessor;
import controllers.modules2.AggregationProcessor;
import controllers.modules2.AggregationProcessorOld;
import controllers.modules2.ColumnSelectProcessor;
import controllers.modules2.DataFillerProcessor;
import controllers.modules2.DateFormatMod;
import controllers.modules2.DynamicAggregationPipeProcessor;
import controllers.modules2.DynamicAggregationProcessor;
import controllers.modules2.FirstValuesProcessor;
import controllers.modules2.FullStreamProcessor;
import controllers.modules2.GapProcessor;
import controllers.modules2.InvertProcessor;
import controllers.modules2.LinearProcessor;
import controllers.modules2.LogProcessor;
import controllers.modules2.MinMaxProcessor;
import controllers.modules2.MultiplyProcessor;
import controllers.modules2.PassthroughProcessor;
import controllers.modules2.RangeCleanProcessor;
import controllers.modules2.RawProcessor;
import controllers.modules2.RawWideProcessor;
import controllers.modules2.RelationalOperationProcessor;
import controllers.modules2.RelationalSummaryProcessor;
import controllers.modules2.RepeatCleanProcessor;
import controllers.modules2.SplinesPullProcessor;
import controllers.modules2.SplinesV1PullProcessor;
import controllers.modules2.SplinesV3PullProcessor;
import controllers.modules2.SqlPullProcessor;
import controllers.modules2.SumStreamProcessor;
import controllers.modules2.SumStreamProcessor2;
import controllers.modules2.TimeAverage3Processor;
import controllers.modules2.TimeAverageProcessor;
import controllers.modules2.VariabilityCleanProcessor;
import controllers.modules2.framework.chain.FTranslatorValuesToCsv;
import controllers.modules2.framework.chain.FTranslatorValuesToJson;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.RemoteProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Provider;

public class RawProcessorFactory implements Provider<ProcessorSetup> {

    public static final ThreadLocal<String> threadLocal = new ThreadLocal<String>();

    private Map<String, Class<?>> nameToClazz = new HashMap<String, Class<?>>();
    private Set<String> moduleNamesToForward = new HashSet<String>();
    private Injector injector;

    private Map<String, PullProcessor> pullProcessors = new HashMap<String, PullProcessor>();

    public RawProcessorFactory() {
        nameToClazz.put("getdataV1", SqlPullProcessor.class);
        nameToClazz.put("logV1", LogProcessor.class);
        nameToClazz.put("splinesV1", SplinesV1PullProcessor.class);
        nameToClazz.put("splinesV2", SplinesPullProcessor.class);
        nameToClazz.put("splinesV3", SplinesV3PullProcessor.class);
        nameToClazz.put("gapV1", GapProcessor.class);
        nameToClazz.put("linearV1", LinearProcessor.class);
        nameToClazz.put("rangecleanV1", RangeCleanProcessor.class);
        nameToClazz.put("variabilitycleanV1", VariabilityCleanProcessor.class);
        nameToClazz.put("repeatcleanV1", RepeatCleanProcessor.class);
        nameToClazz.put("timeaverageV2", TimeAverageProcessor.class);
        nameToClazz.put("timeaverageV3", TimeAverage3Processor.class);
        nameToClazz.put("invertV1", InvertProcessor.class);
        nameToClazz.put("passthroughV1", PassthroughProcessor.class);
        nameToClazz.put("rawdataV1", RawProcessor.class);
        nameToClazz.put("rawWideV1", RawWideProcessor.class);
        //nameToClazz.put("demux", StreamsProcessor.class);
        nameToClazz.put("firstvaluesV1", FirstValuesProcessor.class);
        nameToClazz.put("minmaxV1", MinMaxProcessor.class);
        nameToClazz.put("multiplyV1", MultiplyProcessor.class);
        nameToClazz.put("sumstreamsV1", SumStreamProcessor.class);
        nameToClazz.put("sumstreamsV2", SumStreamProcessor2.class);
        nameToClazz.put("csv", FTranslatorValuesToCsv.class);
        nameToClazz.put("json", FTranslatorValuesToJson.class);
        nameToClazz.put("relational", RelationalOperationProcessor.class);
        nameToClazz.put("relationalV1", RelationalOperationProcessor.class);
        nameToClazz.put("columnselect", ColumnSelectProcessor.class);
        nameToClazz.put("columnselectV1", ColumnSelectProcessor.class);
        nameToClazz.put("aggregation", AggregationProcessorOld.class);
        nameToClazz.put("aggregationV1", AggregationProcessor.class);
        nameToClazz.put("dynamicaggregationV1", DynamicAggregationProcessor.class);
        nameToClazz.put("aggregationpipeV1", AggregationPipeProcessor.class);
        nameToClazz.put("dynamicaggregationpipeV1", DynamicAggregationPipeProcessor.class);
        nameToClazz.put("fillerV1", DataFillerProcessor.class);
        nameToClazz.put("dateformatV1", DateFormatMod.class);
        nameToClazz.put("relationalsummaryV1", RelationalSummaryProcessor.class);

        nameToClazz.put("streamV1", FullStreamProcessor.class);

        for (Entry<String, Class<?>> entry : nameToClazz.entrySet()) {
            String key = entry.getKey();
            Class<?> value = entry.getValue();
            if (PullProcessor.class.isAssignableFrom(value)) {
                PullProcessor pullProc;
                try {
                    pullProc = (PullProcessor) value.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("module failed to load=" + value, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("module failed to load=" + value, e);
                }
                if (pullProc.getGuiMeta() != null) {
                    pullProcessors.put(key, pullProc);
                }
            }
        }

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
        if (moduleName == null) {
            throw new IllegalArgumentException("Please call ProcessorFactory.threadLocal.set(moduleName)");
        }
        Class<?> clazz = nameToClazz.get(moduleName);
        if (clazz == null) {
            if (moduleNamesToForward.contains(moduleName)) {
                return injector.getInstance(RemoteProcessor.class);
            }
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

    public Map<String, PullProcessor> fetchPullProcessors() {
        return pullProcessors;
    }

    public List<String> fetchNonTerminalModules() {
        List<String> keys = new ArrayList<String>();
        for (Entry<String, PullProcessor> entry : pullProcessors.entrySet()) {
            if (entry.getValue().getGuiMeta().getNumChildren() != NumChildren.NONE) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public List<MetaInformation> fetchAllModules() {
        List<MetaInformation> keys = new ArrayList<MetaInformation>();
        for (Entry<String, PullProcessor> entry : pullProcessors.entrySet()) {
            MetaInformation meta = entry.getValue().getGuiMeta();
            meta.setModuleId(entry.getKey());
            keys.add(meta);
        }
        return keys;
    }

    public List<MetaInformation> fetchGuiOrganizedModules() {
        HashMap<String, List<Entry<String, PullProcessor>>> groups = new HashMap<String, List<Entry<String, PullProcessor>>>();
        List<MetaInformation> keys = new ArrayList<MetaInformation>();
        for (Entry<String, PullProcessor> entry : pullProcessors.entrySet()) {
            if (!groups.containsKey(entry.getValue().getGuiMeta().getModuleType())) {
                groups.put(entry.getValue().getGuiMeta().getModuleType(), new ArrayList<Entry<String, PullProcessor>>());
            }
            groups.get(entry.getValue().getGuiMeta().getModuleType()).add(entry);
        }
        for (String groupname : groups.keySet()) {
            keys.add(new MetaInformation(null, null, false, groupname, false, "group"));
            for (Entry<String, PullProcessor> entry : groups.get(groupname)) {
                MetaInformation meta = entry.getValue().getGuiMeta();
                meta.setModuleId(entry.getKey());
                keys.add(meta);
            }
        }
        return keys;
    }
}
