package robot;

import gov.nrel.util.StartupDetailed;
import gov.nrel.util.StartupGroups;
import gov.nrel.util.StartupRelational;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import models.EntityUser;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VersioningTests {
	
	private final static Logger log = LoggerFactory.getLogger(VersioningTests.class);

	@Test
	public void registerPostAndGet() throws JsonGenerationException, JsonMappingException, IOException {
		
		Random r = new Random(System.currentTimeMillis());
		long id = r.nextLong();
		DefaultHttpClient httpClient = new DefaultHttpClient();

		String tableName = StartupRelational.RELATIONAL_NAME;
		Map<String, Object> point = StartupRelational.createDataPoint(tableName, id, "joe", "junkRoom", -5, false);
		point.put("_user", "dean");
		point.put("_description", "initial import of data");
		point.put("_state", "error");
		postNewDataPoint(httpClient, point, 200);

		Map<String, Object> pointX = StartupRelational.createDataPoint(tableName, id, "joe", "junkRoom56", 45, false);
		postNewDataPoint(httpClient, pointX, 400);
		log.info("data point not posted");

		Map<String, Object> point2 = StartupRelational.createDataPoint(tableName, id, "joe", "junkRoom2", -20, true);
		postNewDataPoint(httpClient, point2, 200);

		String requestUri = "/api/historyV1/"+tableName+"/"+id;
		EntityUser dean = StartupDetailed.DEAN;
		String theString1 = Utility.sendRequest(httpClient, requestUri, dean.getUsername(), dean.getApiKey());
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> result1 = (Map<String, Object>) mapper.readValue(theString1, Object.class);
		List<Object> history1 = (List<Object>) result1.get("history");
		validateFirstHistory(history1);
		
		Map<String, Object> point3 = StartupRelational.createDataPoint(tableName, id, "joe", "junkRoom3", -30, true);
		postNewDataPoint(httpClient, point3, 200);
		
		String theString = Utility.sendRequest(httpClient, requestUri, dean.getUsername(), dean.getApiKey());
		
		Map<String, Object> result = (Map<String, Object>) mapper.readValue(theString, Object.class);
		List<Object> history = (List<Object>) result.get("history");
		
		String first = validateFirstHistory(history);
		
		Assert.assertEquals(2, history.size());
		
		Map<String, Object> historyEntry = (Map<String, Object>) history.get(1);
		String time = historyEntry.keySet().iterator().next();
		Assert.assertNotNull(time);
		
		//sorted results....
		int sortResult = first.compareTo(time);
		Assert.assertTrue(sortResult < 0);
		
		Map<String, Object> entries = (Map<String, Object>) historyEntry.get(time);
		Assert.assertEquals("junkRoom2", entries.get("building"));
		Assert.assertNull(entries.get("_state"));
		


		Map<String, Object> current = (Map<String, Object>) result.get("current");
		Assert.assertEquals("junkRoom3", current.get("building"));


	}

	private String validateFirstHistory(List<Object> history) {
		Map<String, Object> historyEntry1 = (Map<String, Object>) history.get(0);
		String first = historyEntry1.keySet().iterator().next();
		Map<String, Object> entries2 = (Map<String, Object>) historyEntry1.get(first);
		log.info("entries="+entries2);
		Assert.assertEquals("junkRoom", entries2.get("building"));
		Assert.assertEquals("error", entries2.get("_state"));
		return first;
	}

	public static void postNewDataPoint(DefaultHttpClient httpclient, Map result, int code) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(result);
		EntityUser dean = StartupDetailed.DEAN;
		Utility.sendPostRequestImpl(httpclient, "http://localhost:" + Utility.retrievePlayServerPort() + "/api/postdataV1", json, dean.getUsername(), dean.getApiKey(), code);
	}

	
}
