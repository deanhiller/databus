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

public class TestSplineOld {
//	@Test
//	public void hitSplineModule() throws ClientProtocolException, IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		String requestUri = "/api/splineV1/5/5/rawdataV1/fakeTimeSeries/5512/5546";
//		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
//		
//		ObjectMapper mapper = new ObjectMapper();		
//		Map root = mapper.readValue(theString, Map.class);
//		List rows = (List) root.get("data");
//		Assert.assertEquals(7, rows.size());
//	}
//	
//	@Test
//	public void hitSplineModule2() throws ClientProtocolException, IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		String requestUri = "/api/splineV1/5/5/rawdataV1/fakeTimeSeries/5510/5546";
//		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
//		
//		ObjectMapper mapper = new ObjectMapper();		
//		Map root = mapper.readValue(theString, Map.class);
//		List rows = (List) root.get("data");
//		Assert.assertEquals(8, rows.size());
//	}
//	
//	@Test
//	public void hitSplineModuleOnlyOneDataPointAtStart() throws ClientProtocolException, IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		//only one value in this window at 3621, it has value 3.0
//		String requestUri = "/api/splineV1/5/4/rawdataV1/fakeTimeSeries2/3570/3625";
//		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
//		
//		ObjectMapper mapper = new ObjectMapper();		
//		Map root = mapper.readValue(theString, Map.class);
//		List rows = (List) root.get("data");
//		Assert.assertEquals(12, rows.size());
//		Assert.assertEquals((Double)((Map)rows.get(0)).get("value"), new Double(3.0));
//	}
//	
//	@Test
//	public void hitSplineModuleOnlyOneDataPointAtEnd() throws ClientProtocolException, IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		String requestUri = "/api/splineV1/50/4/rawdataV1/fakeTimeSeries2/15850/16000";
//		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
//		
//		ObjectMapper mapper = new ObjectMapper();		
//		Map root = mapper.readValue(theString, Map.class);
//		List rows = (List) root.get("data");
//		Assert.assertEquals(4, rows.size());
//	}
//	
//	@Test
//	public void hitSplineModuleLastValueDropped() throws ClientProtocolException, IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		String requestUri = "/api/splineV1/50/4/rawdataV1/fakeTimeSeries2/15500/16000";
//		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
//		
//		ObjectMapper mapper = new ObjectMapper();		
//		Map root = mapper.readValue(theString, Map.class);
//		List rows = (List) root.get("data");
//		Assert.assertEquals(11, rows.size());
//	}
}
