package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import controllers.modules.mocks.MockProcessor;
import controllers.modules2.EmptyFlag;
import controllers.modules2.MinMaxProcessor;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessor;

public class TestTimeAveProcessor {

	@Test
	public void testProcessor() {
		long[] times    = new long[]   {    1,   2,    3,    4,    5,   25,   26,   27 };
		Double[] values = new Double[] { 44.0, 5.0, 33.0,  5.0,  8.0,  8.0,  5.0,  5.0 };
		
		TimeAverageProcessor processor = new TimeAverageProcessor();
		String path = "timeaverageV2/10/2/rawdata/TABLE/-20/40";
		runTest(times, values, processor, path, true);
		
		runTest(times, values, processor, path, false);
	}

	private void runTest(long[] times, Double[] values, TimeAverageProcessor processor, String path, boolean testPush) {
		List<TSRelational> rows = formRows(times, values);
		List<TSRelational> realResults;
		if(testPush)
			realResults = runProcessor(rows, processor, path);
		else
			realResults = runPullProcessor(rows, processor, path);
		
		Assert.assertEquals(6, realResults.size());
		
		TSRelational tsRelational = realResults.get(0);
		Assert.assertEquals(-8, tsRelational.getTime());
		Assert.assertNull(tsRelational.get("value"));
		
		TSRelational ts2 = realResults.get(1);
		Assert.assertEquals(2, ts2.getTime());
		BigDecimal dec = (BigDecimal) ts2.get("value");
		Assert.assertEquals(24.5, dec.doubleValue());
		
		TSRelational ts3 = realResults.get(2);
		Assert.assertEquals(12, ts3.getTime());
		Assert.assertEquals(new BigDecimal("15.3333333333"), ts3.get("value"));
	}

	private List<TSRelational> runPullProcessor(List<TSRelational> rows,
			TimeAverageProcessor processor, String path) {
		MockProcessor mock = new MockProcessor();
		mock.setRows(rows);
		processor.setChild(mock);
		VisitorInfo visitor = new VisitorInfo(null, null, false, null);
		processor.init(path, mock, visitor, new HashMap<String, String>());
		
		List<TSRelational> realResults = new ArrayList<TSRelational>();
		while(true) {
			ReadResult res = processor.read();
			if(res.isEndOfStream())
				break;
			
			realResults.add(res.getRow());
		}
		return realResults;
	}

	private List<TSRelational> runProcessor(List<TSRelational> rows,
			PushProcessor processor, String path) {
		EmptyFlag flag = new EmptyFlag();
		MockProcessor mock = new MockProcessor();
		VisitorInfo visitor = new VisitorInfo(null, null, false, null);
		processor.init(path, mock, visitor, new HashMap<String, String>());

		for(TSRelational row : rows) {
			processor.incomingChunk(null, row, flag);
		}
		
		processor.complete(null);
		List<TSRelational> realResults = mock.getRows();
		return realResults;
	}

	static List<String> createParams(Object ... params) {
		List<String> result = new ArrayList<String>();
		for(Object p : params) {
			result.add(p+"");
		}
		return result;
	}

	static List<TSRelational> formRows(long[] times, Double[] values) {
		if(times.length != values.length)
			throw new IllegalArgumentException("arrays must be the same length");
		List<TSRelational> rows = new ArrayList<TSRelational>();
		for(int i = 0; i < times.length; i++) {
			TSRelational map = new TSRelational();
			map.setTime(times[i]);
			map.put("value", new BigDecimal(values[i]+""));
			rows.add(map);
		}
		return rows;
	}
}
