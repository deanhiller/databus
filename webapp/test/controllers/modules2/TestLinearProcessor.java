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

public class TestLinearProcessor {

	@Test
	public void testSplineV2BasicValue() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/linearV1(interval=100)/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(4, object.size());
	}

	@Test
	public void testProcessorLinear() {
		long[] times    = new long[]   {    1,   2,    3,    5,    15,   25,   28,   35 };
		Double[] values = new Double[] { 44.0, 5.0, 33.0,  15.0,  10.0,  20.0,  28.0,  35.0 };
		
		LinearProcessor processor = new LinearProcessor();
		String path = "linearV1/basic/10/2/rawdata/TABLE/-20/40";
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		List<TSRelational> realResults = runTest(times, values, processor, path, false, options );
		
		Assert.assertEquals(3, realResults.size());
		
		TSRelational tsRelational = realResults.get(0);
		Assert.assertEquals(10, tsRelational.getTime());
		BigDecimal dec = (BigDecimal) tsRelational.get("value");
		Assert.assertEquals(0, dec.compareTo(new BigDecimal("12.5")));
	}

	@Test
	public void testProcessor() {
		long[] times    = new long[]   {    1,   2,    3,    4,    5,   25,   26,   27 };
		Double[] values = new Double[] { 44.0, 5.0, 33.0,  5.0,  8.0,  8.0,  5.0,  5.0 };
		
		LinearProcessor processor = new LinearProcessor();
		String path = "linearV1/basic/10/2/rawdata/TABLE/-20/40";
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		List<TSRelational> realResults = runTest(times, values, processor, path, false, options );
		
		Assert.assertEquals(2, realResults.size());
		
		TSRelational tsRelational = realResults.get(0);
		Assert.assertEquals(10, tsRelational.getTime());
		BigDecimal dec = (BigDecimal) tsRelational.get("value");
		Assert.assertEquals(8, dec.intValue());
	}

	private List<TSRelational> runTest(long[] times, Double[] values, LinearProcessor processor, String path, boolean testPush, HashMap<String, String> options) {
		List<TSRelational> rows = formRows(times, values);
		return runPullProcessor(rows, processor, path, options);
	}

	static List<TSRelational> runPullProcessor(List<TSRelational> rows,
			PullProcessorAbstract processor, String path, HashMap<String, String> options) {
		MockProcessor mock = new MockProcessor();
		mock.setRows(rows);
		processor.setChild(mock);
		VisitorInfo visitor = new VisitorInfo(null, null, false, null);
		processor.init(path, mock, visitor, options);
		
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
			map.put("value", new BigDecimal(values[i]+""));
			rows.add(map);
		}
		return rows;
	}
}
