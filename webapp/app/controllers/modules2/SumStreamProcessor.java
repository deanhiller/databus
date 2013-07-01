package controllers.modules2;

import java.math.BigDecimal;
import java.util.List;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

public class SumStreamProcessor extends StreamsProcessor {

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
