package robot;

import gov.nrel.util.StartupDetailed;
import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.commons.lang.StringUtils;
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

import controllers.Tuple;


public class RegisterPostAndGet {
	
	private final static Logger log = LoggerFactory.getLogger(RegisterPostAndGet.class);
	
	private int port = Utility.retrievePlayServerPort();
	ArrayList<String> regionTags = new ArrayList<String>();
	ArrayList<String> buildingTags = new ArrayList<String>();
	ArrayList<String> roomTags = new ArrayList<String>();
	ArrayList<String> typeTags = new ArrayList<String>();
	ArrayList<String> subtypeTags = new ArrayList<String>();
	ArrayList<String> unitTags = new ArrayList<String>();
	ArrayList<String> equipmentTags = new ArrayList<String>();
	ArrayList<String> randomTags = new ArrayList<String>();
	
	@Test
	public void generateABunchOfTablesWithTags() throws JsonGenerationException, JsonMappingException, IOException {
		
		regionTags.addAll(Arrays.asList("r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11","r12"));
		for(int i = 0; i < 100; i++)
			buildingTags.add("b"+i);
		for(int i = 0; i < 1000; i++)
			roomTags.add("r"+i);
		typeTags.addAll(Arrays.asList("analogInput", "analogOutput", "digitalInput", "digitalOutput"));
		subtypeTags.addAll(Arrays.asList("HVAC", "Power", "lighting", "elevator", "solar", "heat pump", "data center", "emergency generator"));
		unitTags.addAll(Arrays.asList("degree F", "degree C", "kelvins", "mhz", "ghz", "rpm", "btu", "kw", "kwh", "watts"));
		equipmentTags.addAll(Arrays.asList("ac unit", "tv monitor", "computer", "led light", "fan", "fireplace", "solar panel"));
		randomTags.addAll(Arrays.asList("tall", "short", "big", "small", "blue", "red", "hot", "cold", "happy", "sad"));

		List<Long> timeResults = generateTablesWithRandomTags(6009, 10000);
		System.out.println("times!!!!!!");
		for (int i =0; i< timeResults.size(); i++)
			System.out.println(timeResults.get(i));
		
	}
	
	BigDecimal nrelTopLeftLat = new BigDecimal("39.743426");
	BigDecimal nrelTopLeftLon = new BigDecimal("-105.177516");
	BigDecimal nrelBottomRightLat = new BigDecimal("39.738230");
	BigDecimal nrelBottomRightLon = new BigDecimal("-105.168101");
	
	BigDecimal denverTopLeftLat = new BigDecimal("39.896930");
	BigDecimal denverTopLeftLon = new BigDecimal("-105.194778");
	BigDecimal denverBottomRightLat = new BigDecimal("39.549423");
	BigDecimal denverBottomRightLon = new BigDecimal("-104.693527");
	
	private Tuple getAGeoLocInNREL() {
		BigDecimal somelat = pickANumberInside(nrelTopLeftLat, nrelBottomRightLat);
		BigDecimal somelon = pickANumberInside(nrelTopLeftLon, nrelBottomRightLon);
		Tuple t = new Tuple(""+somelat, ""+somelon);
		return t;
	}

	private Tuple getAGeoLocOutsideNREL() {
		BigDecimal somelat = pickANumberInsideExcludingRange(denverTopLeftLat, denverBottomRightLat, nrelTopLeftLat, nrelBottomRightLat);
		BigDecimal somelon = pickANumberInsideExcludingRange(denverTopLeftLon, denverBottomRightLon, nrelTopLeftLon, nrelBottomRightLon);
		Tuple t = new Tuple(""+somelat, ""+somelon);
		return t;
	}
	
	
	

	private BigDecimal pickANumberInsideExcludingRange(
			BigDecimal first, BigDecimal second,
			BigDecimal excludefirst, BigDecimal excludesecond) {
		BigDecimal result;
		do {
			result = pickANumberInside(first, second);
		} while (!(result.compareTo(excludefirst.min(excludesecond)) >= 0 && result.compareTo(excludefirst.max(excludesecond)) <= 0));
		return result;
	}

	private BigDecimal pickANumberInside(BigDecimal first,
			BigDecimal second) {
		BigDecimal range = first.min(second).subtract(first.max(second));
		BigDecimal scaledRange = range.multiply(new BigDecimal(1000000));
		BigDecimal randomizedVal = new BigDecimal(new Random().nextLong()%scaledRange.longValue());
		return first.min(second).add(randomizedVal.divide(new BigDecimal(1000000)));
	}
	
	private List<Long> generateTablesWithRandomTags(int start, int end) throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		ArrayList<Long> times = new ArrayList<Long>();
		int errorcount = 0;
		
		for (int i = start; i < end; i++) {
			String tableName = "tableWithTags"+i;
			List<String> someTags = selectRandomTags();
			long before = System.currentTimeMillis();
			try{
				Tuple loc = getAGeoLocInNREL();
				if (i%2==0)
					loc = getAGeoLocOutsideNREL();
				registerNewStream(httpclient, tableName, someTags, new BigDecimal(loc.key), new BigDecimal(loc.value));
			}
			catch (Exception e) {
				errorcount++;
			}
			long after = System.currentTimeMillis();
			times.add(after - before);
			System.out.println("  ^^^^^^ time for table tableWithTags"+i+":  "+(after-before));
			if (errorcount > 0 ) {
				System.out.println("we got errors!  "+errorcount);
			}
		}
		return times;
	}

	private List<String> selectRandomTags() {
		List someTags = new ArrayList<String>();
		someTags.add(regionTags.get(new Random().nextInt(regionTags.size())));
		someTags.add(buildingTags.get(new Random().nextInt(buildingTags.size())));
		someTags.add(roomTags.get(new Random().nextInt(roomTags.size())));
		someTags.add(typeTags.get(new Random().nextInt(typeTags.size())));
		someTags.add(subtypeTags.get(new Random().nextInt(subtypeTags.size())));
		someTags.add(unitTags.get(new Random().nextInt(unitTags.size())));
		someTags.add(equipmentTags.get(new Random().nextInt(equipmentTags.size())));
		someTags.add(randomTags.get(new Random().nextInt(randomTags.size())));

		return someTags;
	}

	public void registerPostAndGet() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String tableName = "pinkBlobFromMarsData"+r;
		registerNewStream(httpclient, tableName+"1jsc", Arrays.asList("a"), null, null);
		registerNewStream(httpclient, tableName+"2jsc", Arrays.asList("a", "b"), null, null);
		registerNewStream(httpclient, tableName+"3jsc", Arrays.asList("a", "b", "c"), null, null);
		registerNewStream(httpclient, tableName, Arrays.asList("e", "b", "a", "c"), null, null);

		String apiKey = StartupGroups.ROBOT_KEY;
		postNewDataPoint(httpclient, 30, 34.5, apiKey, tableName);
		postNewDataPoint(httpclient, 65, 23.3, apiKey, tableName);
		postNewDataPoint(httpclient, 51, 23.2, apiKey, tableName);
		postNewDataPoint(httpclient, 11, 23.2, apiKey, tableName);

		List<Map> tuples = new ArrayList<Map>();
		tuples.add(createPoint(10, 23.2, apiKey, tableName));
		tuples.add(createPoint(0, 23.2, apiKey, tableName));
		tuples.add(createPoint(53, 23.2, apiKey, tableName));
		
		Map<String, List<Map>> map = new HashMap<String, List<Map>>();
		map.put("_dataset", tuples);
		postNewBatch(httpclient, map);
		//postNewDataPoint(httpclient, 10, "grey", 23.2, postKey);
		//postNewDataPoint(httpclient, 0, "grey", 23.2, postKey);
		//postNewDataPoint(httpclient, 53, "grey", 23.2, postKey);
		
		getData(tableName, apiKey, httpclient);
	}

	@SuppressWarnings("rawtypes")
	private void getData(String tableName, String getKey, DefaultHttpClient httpClient) throws ClientProtocolException, IOException {
		String requestUri = "/api/rawdataV1/"+tableName+"/-60/60";
		String theString = Utility.sendRequest(httpClient, requestUri, StartupGroups.ROBOT_USER, getKey);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Map map = (Map) root;
		List dataPoints = (List) map.get("data");
		Assert.assertEquals(6, dataPoints.size());
		Map point1 = (Map) dataPoints.get(0);
		Map point2 = (Map) dataPoints.get(1);
		Assert.assertEquals(0, point1.get("time"));
		Assert.assertEquals(10, point2.get("time"));
		
		log.info("class="+root);
	}
	
	static void postNewBatch(DefaultHttpClient httpclient, Map tuples) 
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(tuples);
		Utility.sendPostRequest(httpclient, "http://localhost:" + Utility.retrievePlayServerPort() + "/postdata", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
	}
	
	public static void postNewDataPoint(DefaultHttpClient httpclient, long time,
			double volume, String postKey, String tableName) throws UnsupportedEncodingException,
			IOException, ClientProtocolException {
		Map result = createPoint(time, volume, postKey, tableName);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(result);
		
		Utility.sendPostRequest(httpclient, "http://localhost:" + Utility.retrievePlayServerPort() + "/postdata", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
	}

	private static Map createPoint(long time, double volume, String postKey, String tableName) {
		Map result = new HashMap();
		result.put("_postKey", postKey);
		result.put("_tableName", tableName);
		result.put("time", time+"");
		result.put("value", volume+"");
		return result;
	}
	
	private RegisterResponseMessage registerNewStream(DefaultHttpClient httpclient, String tableName, List<String> tags, BigDecimal lat, BigDecimal lon) 
			throws IOException, JsonGenerationException,
			JsonMappingException, UnsupportedEncodingException,
			ClientProtocolException, JsonParseException {
		String json = createJsonForRequest(tableName, false, tags, lat, lon);
		
		String theString = Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(theString, RegisterResponseMessage.class);
		return resp;
	}

	public static String createJsonForRequest(String tableName, boolean isForLogging, List<String> tags, BigDecimal lat, BigDecimal lon) throws IOException,
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
		msg.setModelName(tableName);
		msg.setSchema(StartupDetailed.GROUP1);
		if (tags != null && tags.size() > 0)
			msg.setTags(tags);
		
		if (lat != null && lon != null && !lat.equals(new BigDecimal("0.0")) && !lon.equals(new BigDecimal(0.0))) {
			msg.setLat(lat);
			msg.setLon(lon);
		}
		
		if(isForLogging) //else leave null as this is an optional field
			msg.setIsForLogging(isForLogging);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		createColumn(cols, "value", "BigDecimal", "oei:volume", true, false);
		msg.setColumns(cols);

		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, msg);
		String json = out.toString();
		return json;
	}

	//{"name":"time","dataType":"BigInteger","semanticType":"oei:timestamp","isIndex":false,"isPrimaryKey":true,"semantics":[]},

	static void createColumn(List<DatasetColumnModel> cols, String name,
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
