package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.message.StreamModule;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.StreamsProcessor;

public class AggregationPipeProcessor extends StreamsProcessor {

	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.MANY, false, "Aggregation Pipe", false, "Aggregation");

	static {
		
		metaInfo.setDescription("This module takes many streams(that don't need to be aligned) and combines them into a single stream with rows like time, value1, value2, value3 passing that to the next module where some rows may have some values missing when there is no value for that timestamp");
	}

	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}
	
	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		String res = super.init(path, nextInChain, visitor, options);
		
		return res;
	}
	
	@Override
	protected ReadResult process(List<TSRelational> rows) {
		Long timeCompare = null;
		TSRelational ts = new TSRelational();
		
		for(TSRelational row : rows) {
			if(row == null)
				continue;

			Long time = getTime(row);
			if(timeCompare == null) {
				timeCompare = time;
			} else if(!timeCompare.equals(time)) {
				throw new IllegalStateException("It appears you did not run spline before summing the streams as times are not lining up.  t1="+time+" t2="+timeCompare);
			}

			for(Entry<String, Object> entry : row.entrySet()) {
				if(timeColumn.equals(entry.getKey())) 
					continue; //skip time column
				ts.put(entry.getKey(), entry.getValue());
			}
		}

		if(timeCompare == null) {
			return new ReadResult();
		}
		
		setTime(ts, timeCompare);
		ReadResult res = new ReadResult(null, ts);
		return res;
	}
	
	@Override
	public ReadResult read() {
		return super.read();
	}
	
}
