package controllers.modules2;

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

public class TestFirstValuesProcessor {

	@Test
	public void testProc() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/firstvaluesV1/10/rawdataV1/fakeTimeSeries2?reverse=true";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(10, rows.size());
		Map<String, Integer> dataPoint = (Map<String, Integer>) rows.get(0);
		Assert.assertEquals(new Integer(15891), dataPoint.get("time"));
		Assert.assertEquals(new Integer(199), dataPoint.get("value"));
		
		Map<String, Integer> dataPoint2 = (Map<String, Integer>) rows.get(rows.size()-1);
		Assert.assertEquals(new Integer(15351), dataPoint2.get("time"));
		Assert.assertEquals(new Integer(190), dataPoint2.get("value"));		
	}
	
	@Test
	public void testForward() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/firstvaluesV1/10/rawdataV1/fakeTimeSeries2";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(10, rows.size());
		Map<String, Integer> dataPoint = (Map<String, Integer>) rows.get(0);
		Assert.assertEquals(new Integer(3456), dataPoint.get("time"));
		Assert.assertEquals(new Integer(0), dataPoint.get("value"));
		
		Map<String, Integer> dataPoint2 = (Map<String, Integer>) rows.get(rows.size()-1);
		Assert.assertEquals(new Integer(3951), dataPoint2.get("time"));
		Assert.assertEquals(new Integer(9), dataPoint2.get("value"));		
	}
	
}
