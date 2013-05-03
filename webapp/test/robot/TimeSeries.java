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


public class TimeSeries {
	
	private final static Logger log = LoggerFactory.getLogger(TimeSeries.class);
	
	private int port = Utility.retrievePlayServerPort();
	
	@Test
	public void registerPostAndGet() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String tableName = "timeSeriesFromMarsData"+r;
		registerNewStream(httpclient, tableName);

		String postKey = StartupGroups.ROBOT_KEY;
		postNewDataPoint(httpclient, 30, "pink", 34.5, tableName);
		postNewDataPoint(httpclient, 65, "green", 23.3, tableName);
		postNewDataPoint(httpclient, 51, "grey", 23.2, tableName);
		postNewDataPoint(httpclient, 11, "grey", 23.2, tableName);

		List<Map> tuples = new ArrayList<Map>();
		tuples.add(createPoint(10, 23.2, tableName));
		tuples.add(createPoint(0, 23.5, tableName));
		tuples.add(createPoint(53, 23.2, tableName));
		
		Map<String, List<Map>> map = new HashMap<String, List<Map>>();
		map.put("_dataset", tuples);
		RegisterPostAndGet.postNewBatch(httpclient, map);
		//postNewDataPoint(httpclient, 10, "grey", 23.2, postKey);
		//postNewDataPoint(httpclient, 0, "grey", 23.2, postKey);
		//postNewDataPoint(httpclient, 53, "grey", 23.2, postKey);
		
		getData(tableName, postKey, httpclient);
	}

	@SuppressWarnings("rawtypes")
	private void getData(String tableName, String getKey, DefaultHttpClient httpClient) throws ClientProtocolException, IOException {
		String requestUri = "/api/rawdataV1/"+tableName+"/-60/60";
		
		String theString = Utility.sendRequest(httpClient, requestUri, StartupGroups.ROBOT_USER, getKey);

		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List object = (List) map.get("data");
		Assert.assertEquals(6, object.size());
		
		Map map1 = (Map) object.get(0);
		Map map2 = (Map) object.get(1);
		Double point1 = (Double) map1.get("value");
		Double point2 = (Double) map2.get("value");
		Assert.assertTrue(point1.equals(new Double(23.5)));
		Assert.assertTrue(point2.equals(new Double(23.2)));
		
		log.info("class="+root);
	}
	
	public static void postNewDataPoint(DefaultHttpClient httpclient, long time,
			String color, double volume, String tableName) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		Map result = createPoint(time, volume, tableName);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(result);

		Utility.sendPostRequest(httpclient, "http://localhost:" + Utility.retrievePlayServerPort() + "/postdata", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
	}

	private static Map createPoint(long time, double volume, String tableName) {
		Map result = new HashMap();
		result.put("_tableName", tableName);
		result.put("time", time+"");
		result.put("value", volume);
		return result;
	}
	
	private RegisterResponseMessage registerNewStream(DefaultHttpClient httpclient, String tableName) 
			throws IOException, JsonGenerationException,
			JsonMappingException, UnsupportedEncodingException,
			ClientProtocolException, JsonParseException {
		String json = createJsonForRequest(tableName);
		String theString = Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(theString, RegisterResponseMessage.class);
		return resp;
	}

	private String createJsonForRequest(String tableName) throws IOException,
			JsonGenerationException, JsonMappingException {
//		{"datasetType":"STREAM",
//		 "modelName":"timeSeriesForPinkBlobZ",
//		 "groups":["supa"],
//		 "columns":[
//		      {"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},
//		      {"name":"value","dataType":"BigDecimal","semanticType":"oei:volume","isIndex":false,"isPrimaryKey":false,"semantics":[]}
//		      ]
//		}
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		msg.setSchema(StartupDetailed.GROUP1);
		msg.setModelName(tableName);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		createColumn(cols, "value", "BigDecimal", "oei:temparaturecolor", false, false);
		msg.setColumns(cols);

		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, msg);
		String json = out.toString();
		return json;
	}

	//{"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},

	private void createColumn(List<DatasetColumnModel> cols, String name,
			String dataType, String semanticType, boolean isIndex, boolean isPrimaryKey) {
		DatasetColumnModel col = new DatasetColumnModel();
		col.setName(name);
		col.setDataType(dataType);
		col.setSemanticType(semanticType);
		col.setIsIndex(isIndex);
		col.setIsPrimaryKey(isPrimaryKey);
		cols.add(col);
	}

}
