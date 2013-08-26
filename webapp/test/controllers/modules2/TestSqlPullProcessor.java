package controllers.modules2;

import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import robot.Utility;

public class TestSqlPullProcessor {

	@Test
	public void hitSqlQuery() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/json/firstvaluesV1/1000/getdataV1/select+t+from+wideTable+as+t+where+t.timeStart=1367846618000";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		List rows = (List) root.get("data");
		Assert.assertEquals(1, rows.size());
		Map<String, Object> dataPoint = (Map<String, Object>) rows.get(0);
		
		String energyStr = ""+dataPoint.get("energy");
		String volStr = ""+dataPoint.get("volume");
		Assert.assertEquals("2500.0", energyStr);
		Assert.assertEquals(volStr, volStr);
	}

}
