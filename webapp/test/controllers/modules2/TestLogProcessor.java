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
import org.mortbay.log.Log;

import robot.RegisterPostAndGet;
import robot.Utility;

public class TestLogProcessor {

	private int port = Utility.retrievePlayServerPort();
	
	@Test
	public void testSplineV2BasicValue() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		long r = System.currentTimeMillis();
		String tableName = "logTestingTable"+r;
		String json = RegisterPostAndGet.createJsonForRequest(tableName, true, null);
		
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		String requestUri = "/api/logV1/"+tableName+"/splinesV2/basic/5/3/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(20, object.size());
		//									{time=203,         value=24.3713875}
		Assert.assertTrue(theString.contains("\"time\":203,\"value\":24.3713875"));
		
		String request2 = "/api/rawdataV1/"+tableName+"/0/500";
		String result2 = Utility.sendRequest(httpclient, request2, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		Assert.assertEquals(theString, result2);
	}
	
	@Test
	public void testNotLoggingTable() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		long r = System.currentTimeMillis();
		String tableName = "logTestingTable2"+r;
		String json = RegisterPostAndGet.createJsonForRequest(tableName, false, null);
		
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		String requestUri = "/api/logV1/"+tableName+"/splinesV2/basic/5/3/rawdataV1/splineTests1/0/500";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY, 400);
		//Log.info("!!!!!!!!!!!the returned string is -----"+theString);
		//Assert.assertTrue(theString.contains("was not registered as a table for logging"));
	}
}
