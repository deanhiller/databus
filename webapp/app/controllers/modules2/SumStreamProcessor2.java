package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;
import controllers.modules2.framework.procs.StreamsProcessor2;

public class SumStreamProcessor2 extends StreamsProcessor2 {

	private boolean enableNulls;

	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		initializeProps(options);
		return super.init(path, nextInChain, visitor, options);
	}

	public void initializeProps(Map<String, String> options) {
		String nulls = options.get("enableNulls");
		enableNulls = false;
		if(!StringUtils.isEmpty(nulls) && "true".equals(nulls))
			enableNulls = true;
		
		//temporary for testing only(overridden in superclass)
		timeColumn = "time";
		valueColumn = "value";
	}

	@Override
	protected ReadResult process(List<TSRelational> rows) {
		Long timeCompare = null;
		BigDecimal total = BigDecimal.ZERO;
		for(TSRelational row : rows) {
			if(row == null) {
				if(enableNulls)
					total = null;
				continue;
			}

			Long time = getTime(row);
			if(timeCompare == null) {
				timeCompare = time;
			} else if(!timeCompare.equals(time)) {
				throw new IllegalStateException("It appears you did not run spline before summing the streams as times are not lining up.  t1="+time+" t2="+timeCompare);
			}

			BigDecimal val = getValueEvenIfNull(row);
			if(enableNulls && val == null || total == null)
				total = null;
			else if(val != null)
				total = total.add(val);
		}

		if(timeCompare == null) {
			return new ReadResult();
		}
		
		TSRelational ts = new TSRelational();
		setTime(ts, timeCompare);
		setValue(ts, total);
		ReadResult res = new ReadResult(null, ts);
		return res;
	}

}
