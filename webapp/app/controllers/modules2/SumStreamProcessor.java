package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.message.ChartVarMeta;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

public class SumStreamProcessor extends StreamsProcessor {

	private static final String COL_NAME = "outputColumn";
	protected static Map<String, ChartVarMeta> paramMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(paramMeta, NumChildren.MANY, false, "Sum Stream");

	static {
		metaInfo.setDescription("This module takes many streams and adds them up");
	}

	@Override
	public MetaInformation getGuiMeta() {
		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Output Column");
		meta.setNameInJavascript(COL_NAME);
		meta.setRequired(true);
		meta.setHelp("The name of the column to be used in output to the next module up");
		paramMeta.put(meta.getNameInJavascript(), meta);
		return metaInfo;
	}

	@Override
	public void initModule(Map<String, String> options, long start, long end) {
		super.initModule(options, start, end);
		String outputCol = options.get(COL_NAME);
		valueColumn = outputCol;
	}

	@Override
	protected ReadResult process(List<TSRelational> rows) {
		Long timeCompare = null;
		BigDecimal total = BigDecimal.ZERO;
		for(TSRelational row : rows) {
			if(row == null)
				continue;

			Long time = getTime(row);
			if(timeCompare == null) {
				timeCompare = time;
			} else if(!timeCompare.equals(time)) {
				throw new IllegalStateException("It appears you did not run spline before summing the streams as times are not lining up.  t1="+time+" t2="+timeCompare);
			}

			BigDecimal val = getValueEvenIfNull(row);
			if(val != null) {
				total = total.add(val);
			}
		}

		if(timeCompare == null) {
			return new ReadResult();
		}
		
		TSRelational ts = new TSRelational(timeColumn, valueColumn);
		setTime(ts, timeCompare);
		setValue(ts, total);
		ReadResult res = new ReadResult(null, ts);
		return res;
	}
}
