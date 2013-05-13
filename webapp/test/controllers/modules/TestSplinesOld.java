package controllers.modules;

import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import robot.Utility;

public class TestSplinesOld {
	@Test
	public void testSplineBasicValue() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/basic/5/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		Assert.assertTrue(theString.contains("\"time\":205,\"value\":24.0828125000000000"));
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(21, object.size());
	}
	
	//@Test
	public void testSplineLimitDerivativeValue() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/limitderivative/5/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		Assert.assertTrue(theString.contains("\"time\":100,\"value\":100.0"));
		Assert.assertTrue(theString.contains("\"time\":50,\"value\":100.0"));
		Assert.assertTrue(theString.contains("\"time\":250,\"value\":37.5"));
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(101, object.size());
		
//		Map map1 = (Map) object.get(0));
//		Map map2 = (Map) object.get(1);
//		Double point1 = (Double) map1.get("value");
//		Double point2 = (Double) map2.get("value");
//		Assert.assertTrue(point1.equals(new Double(23.5)));
//		Assert.assertTrue(point2.equals(new Double(23.2)));
	}

	//@Test
	public void hitSplineModule() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/basic/5/rawdataV1/fakeTimeSeries/5512/5546";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(7, rows.size());
	}
	
	//@Test
	public void hitSplineModule2() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/basic/5/rawdataV1/fakeTimeSeries/5510/5546";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(8, rows.size());
	}
	
	//@Test
	public void hitSplineModuleOnlyOneDataPointAtStart() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		//only one value in this window at 3621, it has value 3.0
		String requestUri = "/api/splinesV1/basic/5/rawdataV1/fakeTimeSeries2/3570/3625";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(12, rows.size());
		Assert.assertEquals((Double)((Map)rows.get(0)).get("value"), new Double(3.0));
	}
	
	//@Test
	public void hitSplineModuleOnlyOneDataPointAtEnd() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/basic/50/rawdataV1/fakeTimeSeries2/15850/16000";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(4, rows.size());
	}
	
	//@Test
	public void hitSplineModuleLastValueDropped() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/splinesV1/basic/50/rawdataV1/fakeTimeSeries2/15500/16000";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(11, rows.size());
	}
	

}
