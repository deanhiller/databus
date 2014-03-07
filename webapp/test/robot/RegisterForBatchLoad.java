package robot;

import gov.nrel.util.StartupBean;
import gov.nrel.util.StartupDetailed;
import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.GroupKey;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
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
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.test.FunctionalTest;


public class RegisterForBatchLoad {
	
	private final static Logger log = LoggerFactory.getLogger(RegisterForBatchLoad.class);
	
	private int port = Utility.retrievePlayServerPort();
	
	@Test
	public void registerPostAndGet() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String tableName = "pinkBlobFromMarsData"+r;
		registerNewStream(httpclient, tableName, null);

		queryEmptyTable(tableName, httpclient);
		
		//test for 401, no access
		String requestUri = "/api/rawdataV1/"+tableName+"/-"+Long.MAX_VALUE+"/"+Long.MAX_VALUE;
		Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, "1234324234324", 401);
		
		String tableName2 = "pinkBlobFromMarsDataTWO"+r;
		registerNewStream(httpclient, tableName2, null);

		List<Map> tuples = new ArrayList<Map>();
		tuples.add(createPoint(10, 23.2, tableName));
		tuples.add(createPoint(25, 23.2, tableName2));
		tuples.add(createPoint(53, 23.2, tableName));
		
		Map<String, List<Map>> map = new HashMap<String, List<Map>>();
		map.put("_dataset", tuples);
		RegisterPostAndGet.postNewBatch(httpclient, map);
		
		getData(tableName, httpclient);
	}
	
	private void queryEmptyTable(String tableName, DefaultHttpClient httpclient) throws ClientProtocolException, IOException {
		String requestUri = "/api/rawdataV1/"+tableName+"/-"+Long.MAX_VALUE+"/"+Long.MAX_VALUE;

		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List dataPoints = (List) map.get("data");
		Assert.assertEquals(0, dataPoints.size());
	}

	@Test
	public void registerInvalidStream() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String tableName = "pinkBlobFromMarsData"+r;
		//the assert all the way down in sendPostRequestExpectedToFailImpl will verify that this request errors as expected:
		registerInvalidNewStream(httpclient, tableName);
		
	}

	@SuppressWarnings("rawtypes")
	private void getData(String tableName, DefaultHttpClient httpclient) throws ClientProtocolException, IOException {
		String requestUri = "/api/rawdataV1/"+tableName+"/0/60";

		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List dataPoints = (List) map.get("data");
		Assert.assertEquals(2, dataPoints.size());
		Map point1 = (Map) dataPoints.get(0);
		Map point2 = (Map) dataPoints.get(1);
		Assert.assertEquals(10, point1.get("time"));
		Assert.assertEquals(53, point2.get("time"));
		
		log.info("class="+root);
	}

	private static Map createPoint(long time, double volume, String tableName) {
		Map result = new HashMap();
		result.put("_tableName", tableName);
		result.put("time", time+"");
		result.put("value", volume+"");
		return result;
	}
	
	private RegisterResponseMessage registerNewStream(DefaultHttpClient httpclient, String tableName, List<String> tags) 
			throws IOException, JsonGenerationException,
			JsonMappingException, UnsupportedEncodingException,
			ClientProtocolException, JsonParseException {
		String json = RegisterPostAndGet.createJsonForRequest(tableName, false, tags);
		String theString = Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(theString, RegisterResponseMessage.class);
		return resp;
	}
	
	//DatasetType.STREAM MUST only allow time and value.  Try to create one with 'time' and 'volume' and ensure it fails
	private void registerInvalidNewStream(DefaultHttpClient httpclient, String tableName) 
			throws IOException, JsonGenerationException,
			JsonMappingException, UnsupportedEncodingException,
			ClientProtocolException, JsonParseException {
		String json = createInvalidJsonForRequest(tableName);
		Utility.sendPostRequestExpectedToFail(httpclient, "http://localhost:9000/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
	}
	
	//DatasetType.STREAM MUST only allow time and value.  Try to create one with 'time' and 'volume' and ensure it fails
	private String createInvalidJsonForRequest(String tableName) throws IOException,
			JsonGenerationException, JsonMappingException {
		// {"datasetType":"STREAM",
		// "modelName":"timeSeriesForPinkBlobZ",
		// "groups":["supa"],
		// "columns":[
		// {"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},
		// {"name":"volume","dataType":"BigDecimal","semanticType":"oei:volume","isIndex":false,"isPrimaryKey":false,"semantics":[]}
		// ]
		// }
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		msg.setSchema(StartupDetailed.GROUP1);
		msg.setModelName(tableName);

		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		RegisterPostAndGet.createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		RegisterPostAndGet.createColumn(cols, "volume", "BigDecimal", "oei:volume", true, false);
		msg.setColumns(cols);

		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, msg);
		String json = out.toString();
		return json;
	}
	

	//{"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},

}
