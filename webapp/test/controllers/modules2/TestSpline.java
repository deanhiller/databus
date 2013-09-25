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

import controllers.modules.mocks.MockProcessor;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;
import controllers.modules2.framework.procs.PushProcessor;

import robot.Utility;

public class TestSpline {

	@Test
	public void testSplineV2BasicValue() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV2/basic/5/3/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(20, object.size());
		Assert.assertTrue(theString.contains("\"time\":203,\"value\":24.3713875000000000"));
	}
	
	@Test
	public void testProcessor() {
		long[] times    = new long[]   {    1,   2,    3,    4,    5,   25,   26,   27 };
		Double[] values = new Double[] { 44.0, 5.0, 33.0,  5.0,  8.0,  8.0,  5.0,  5.0 };
		
		SplinesPullProcessor processor = new SplinesPullProcessor();
		String path = "splineV1/basic/10/2/rawdata/TABLE/-20/40";
		runTest(times, values, processor, path, false);
	}

	private void runTest(long[] times, Double[] values, SplinesPullProcessor processor, String path, boolean testPush) {
		List<TSRelational> rows = formRows(times, values);
		List<TSRelational> realResults;
		realResults = runPullProcessor(rows, processor, path);
		
		Assert.assertEquals(3, realResults.size());
		
		TSRelational tsRelational = realResults.get(0);
		Assert.assertEquals(2, tsRelational.getTime());
		BigDecimal dec = (BigDecimal) tsRelational.get("value");
		Assert.assertEquals(5, dec.intValue());
		
		TSRelational ts2 = realResults.get(2);
		Assert.assertEquals(22, ts2.getTime());
	}

	static List<TSRelational> runPullProcessor(List<TSRelational> rows,
			PullProcessorAbstract processor, String path) {
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
			TSRelational map = new TSRelational("time", "value");
			map.setTime(times[i]);
			map.put("value", new BigDecimal(values[i]+""));
			rows.add(map);
		}
		return rows;
	}
}
