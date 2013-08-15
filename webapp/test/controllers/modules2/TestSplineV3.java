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

public class TestSplineV3 {

	@Test
	public void testWithNullsThatShouldNOTSplineBecauseBufferToSmall() {
		long[] times    =  new long[40];
		Integer[] values = new Integer[times.length];
		Integer[] values2 =new Integer[times.length];
		String[] fixedVals=new String[times.length];
		
		for(int i = 0; i < times.length; i++) {
			times[i] = i*10+1;
			values[i] = i*10;
			if(i > 5 && i < 30) {
				values2[i] = null;
			} else
				values2[i] = i*10;
		}

		SplinesV3PullProcessor processor = new SplinesV3PullProcessor();
		String path = "splineV3(interval=10,columnsToInterpolate=temp;volume)/rawdata/TABLE/0/400";

		List<TSRelational> rows = formRows(times, values, values2, fixedVals);
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		options.put("maxToStopSplining", "100");//increase so we hit the buffersize limit of 20 before we hit times being too far apart!!!!
		options.put("columnsToInterpolate", "temp;volume");
		List<TSRelational> realResults = runPullProcessor(rows, processor, path, options);
		

		TSRelational rowRes = realResults.get(0);
//		Assert.assertNull(rowRes.get("volume"));
//		Assert.assertNull(rowRes.get("temp"));
	}

	@Test
	public void testWithTimesTooFarApart() {
		long[] times    =  new long[]    {   11,  21,   31,  41,    101,    111,   121,   131 };
		Integer[] values = new Integer[] {   10,  20,   30,  40,    50,    60,   70,   80 };
		Integer[] values2 =new Integer[] {   10,  20,   30,  40,    50,    60,   70,   80 };
		String[] fixedVals=new String[]  {   "10", "20", "30", "40", "50", "60", "70", "80"};

		SplinesV3PullProcessor processor = new SplinesV3PullProcessor();
		String path = "splineV3(interval=10,columnsToInterpolate=temp;volume)/rawdata/TABLE/0/140";

		List<TSRelational> rows = formRows(times, values, values2, fixedVals);
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		options.put("columnsToInterpolate", "temp;volume");
		List<TSRelational> realResults = runPullProcessor(rows, processor, path, options);
		
		for(int i = 0; i < 3; i++) {
			TSRelational r = realResults.get(i);
			Object volume = r.get("volume");
			Object temp = r.get("temp");
			Assert.assertNull(volume);			
			Assert.assertNull(temp);			
		}

		for(int i = 0; i < 2; i++) {
			TSRelational r = realResults.get(i+3);
			Object volume = r.get("volume");
			Object temp = r.get("temp");
			Assert.assertNotNull(volume);			
			Assert.assertNotNull(temp);
		}
		
		for(int i = 0; i < 6; i++) {
			TSRelational row = realResults.get(i+5);
			Object volume = row.get("volume");
			Object temp = row.get("temp");
			Assert.assertNull(volume);			
			Assert.assertNull(temp);
		}
		
		TSRelational last = realResults.get(11);
		Object v= last.get("volume");
		Assert.assertNotNull(v);
	}

	@Test
	public void testNulsAtBeginAndEnd() {
		long[] times    =  new long[]    {   -21,   -11,    1,  11,  21,   31,  41,    51,    61,   71,   81 };
		Integer[] values = new Integer[] {   null, null, null,  10,  20,   30,  40,    50,    60,   70,   80 };
		Integer[] values2 =new Integer[] {    -20,  -10,    0,  10,  20,   30,  40,    50,    60,   null,   null };
		String[] fixedVals=new String[]  {    "a",  "b",  "c","10","20", "30","40",  "50",  "60", "70", "80"};

		SplinesV3PullProcessor processor = new SplinesV3PullProcessor();
		String path = "splineV3(interval=10,columnsToInterpolate=temp;volume)/rawdata/TABLE/-20/90";

		List<TSRelational> rows = formRows(times, values, values2, fixedVals);
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		options.put("columnsToInterpolate", "temp;volume");
		List<TSRelational> realResults = runPullProcessor(rows, processor, path, options);
		
		TSRelational rowRes = realResults.get(0);
		Assert.assertNull(rowRes.get("volume"));
		Assert.assertNull(rowRes.get("temp"));
		
		for(int i = 0; i < 4; i++) {
			TSRelational r = realResults.get(i+1);
			Object volume = r.get("volume");
			Object temp = r.get("temp");
			Assert.assertNotNull(volume);			
			Assert.assertNull(temp);			
		}

		for(int i = 0; i < 3; i++) {
			TSRelational r = realResults.get(i+5);
			Object volume = r.get("volume");
			Object temp = r.get("temp");
			Assert.assertNotNull(volume);			
			Assert.assertNotNull(temp);
		}
		
		for(int i = 0; i < 3; i++) {
			int index = realResults.size()-i-1;
			TSRelational row = realResults.get(index);
			Object val = row.get("volume");
			Assert.assertNull(val);			
		}

		TSRelational r = realResults.get(8);
		Assert.assertNull(r.get("volume"));
		Assert.assertNotNull(r.get("temp"));
	}

	@Test
	public void testWithNullsThatShouldSpline() {
		long[] times    =  new long[]    {   11,  21,   31,  41,    51,    61,   71,   81 };
		Integer[] values = new Integer[] {   10,  20,   30,  40,  null,    60,   70,   80 };
		Integer[] values2 =new Integer[] {   10,  20,   30,  40,    50,  null,   70,   80 };
		String[] fixedVals=new String[]  {   "10", "20", "30", "40", "50", "60", "70", "80"};

		SplinesV3PullProcessor processor = new SplinesV3PullProcessor();
		String path = "splineV3(interval=10,columnsToInterpolate=temp;volume)/rawdata/TABLE/-20/100";

		List<TSRelational> rows = formRows(times, values, values2, fixedVals);
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("interval", "10");
		options.put("columnsToInterpolate", "temp;volume");
		List<TSRelational> realResults = runPullProcessor(rows, processor, path, options);
		
		//the beginning points should be null...
		for(int i = 0; i < 5; i++) {
			TSRelational row = realResults.get(i);
			Object val = row.get("volume");
			Assert.assertNull(val);
		}
		
		//none of the middle values should be null even though there is nulls in the data
		for(int i = 5; i < 5+5; i++) {
			TSRelational row = realResults.get(i);
			Object val = row.get("volume");
			Object temp = row.get("temp");
			Assert.assertNotNull(val);			
			Assert.assertNotNull(temp);
		}
		
		for(int i = 0; i < 3; i++) {
			int index = realResults.size()-i-1;
			TSRelational row = realResults.get(index);
			Object val = row.get("volume");
			Assert.assertNull(val);			
		}
		//The end points should be null
		
		TSRelational ts2 = realResults.get(5);
		Assert.assertEquals(30, ts2.getTime());
		BigDecimal dec = (BigDecimal) ts2.get("temp");
		Assert.assertTrue(dec.compareTo(new BigDecimal("29")) == 0);
	}

	static List<TSRelational> runPullProcessor(List<TSRelational> rows,
			PullProcessorAbstract processor, String path, HashMap<String, String> options2) {
		MockProcessor mock = new MockProcessor();
		mock.setRows(rows);
		processor.setChild(mock);
		VisitorInfo visitor = new VisitorInfo(null, null, false);
		processor.init(path, mock, visitor, options2);
		
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

	static List<TSRelational> formRows(long[] times, Integer[] values, Integer[] values2, String[] fixedVals) {
		if(times.length != values.length || values.length != values2.length)
			throw new IllegalArgumentException("arrays must be the same length");
		List<TSRelational> rows = new ArrayList<TSRelational>();
		for(int i = 0; i < times.length; i++) {
			TSRelational map = new TSRelational("time", "value");
			map.setTime(times[i]);
			if(values[i] == null)
				map.put("temp", null);
			else
				map.put("temp", new BigDecimal(values[i]+""));
			if(values2[i] == null)
				map.put("volume", null);
			else
				map.put("volume", new BigDecimal(values2[i]+""));
			map.put("constant", fixedVals[i]);
			rows.add(map);
		}
		return rows;
	}
}
