package controllers.modules2;

import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import robot.Utility;

import controllers.modules.mocks.MockProcessor;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class TestSumStreamsV2Processor {

	@Test
	public void testProcessorLinear() {
		long[] times    = new long[]   {    10,   20,    30,    50,    60,   70,   80,   90 };
		Double[] values = new Double[] { 44.0, 5.0, null,  15.0,  10.0,  20.0,  null,  35.0 };
		long[] times2    = new long[]   {                30,    50,    60,   70,   80,   90 };
		Double[] values2 = new Double[] {              44.0,  15.0,  null,  20.0,  28.0,  35.0 };		
		
		SumStreamProcessor2 processor = new SumStreamProcessor2();
		String path = "xxxxxxxxrawdata/TABLE/-20/40";
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("enableNulls", "true");
		List<TSRelational> realResults = runTest(times, values, times2, values2, processor, path, false, options );
		
		Assert.assertEquals(8, realResults.size());
		
		TSRelational tsRelational = realResults.get(0);
		Assert.assertEquals(10, tsRelational.getTime());
		BigDecimal dec = (BigDecimal) tsRelational.get("value");
		Assert.assertNull(dec);
		
		
		tsRelational = realResults.get(6);
		dec = (BigDecimal) tsRelational.get("value");
		Assert.assertNull(dec);
	}

	private List<TSRelational> runTest(long[] times, Double[] values, long[] times2, Double[] values2, SumStreamProcessor2 processor, String path, boolean testPush, HashMap<String, String> options) {
		List<TSRelational> rows = formRows(times, values);
		MockProcessor mock = new MockProcessor();
		mock.setRows(rows);
		processor.addChild(mock);

		List<TSRelational> rows2 = formRows(times2, values2);
		MockProcessor mock2 = new MockProcessor();
		mock2.setRows(rows2);
		processor.addChild(mock2);
		
		processor.initializeProps(options);
		return runPullProcessor(rows, processor);
	}

	static List<TSRelational> runPullProcessor(List<TSRelational> rows,
			PullProcessorAbstract processor) {
		List<TSRelational> realResults = new ArrayList<TSRelational>();
		while(true) {
			ReadResult res = processor.read();
			if(res.isEndOfStream())
				break;
			
			realResults.add(res.getRow());
		}
		return realResults;
	}
	
	static List<TSRelational> formRows(long[] times, Double[] values) {
		if(times.length != values.length)
			throw new IllegalArgumentException("arrays must be the same length");
		List<TSRelational> rows = new ArrayList<TSRelational>();
		for(int i = 0; i < times.length; i++) {
			TSRelational map = new TSRelational("time", "value");
			map.setTime(times[i]);
			if(values[i] != null)
				map.put("value", new BigDecimal(values[i]+""));
			rows.add(map);
		}
		return rows;
	}
}
