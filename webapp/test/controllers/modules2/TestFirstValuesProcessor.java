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
	public void testReverse() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/firstvaluesV1/10/rawdataV1/fakeTimeSeries2?reverse=true";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(10, rows.size());
		Map<String, Integer> dataPoint = (Map<String, Integer>) rows.get(0);
		Assert.assertEquals(new Long(1372700837000L), dataPoint.get("time"));
		Assert.assertEquals(new Integer(51), dataPoint.get("value"));
		
		Map<String, Integer> dataPoint2 = (Map<String, Integer>) rows.get(rows.size()-1);
		Assert.assertEquals(new Integer(15411), dataPoint2.get("time"));
		Assert.assertEquals(new Integer(191), dataPoint2.get("value"));		
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
		Assert.assertEquals(new Long(-297843163000L), dataPoint.get("time"));
		Assert.assertEquals(new Integer(50), dataPoint.get("value"));
		
		Map<String, Integer> dataPoint2 = (Map<String, Integer>) rows.get(rows.size()-1);
		Assert.assertEquals(new Integer(3896), dataPoint2.get("time"));
		Assert.assertEquals(new Integer(8), dataPoint2.get("value"));		
	}
	
}
