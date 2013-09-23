package robot;

import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.message.RegisterAggregation;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.Parsing;

public class ModuleTests {
	
	private final static Logger log = LoggerFactory.getLogger(ModuleTests.class);
	
	private int port = Utility.retrievePlayServerPort();

	@Test
	public void hitStdDevModuleNoRowCountLowVariance() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/stddevV1/0/.1/rawdataV1/fakeTimeSeries/333/8555";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(9, rows.size());
	}
	
	@Test
	public void hitStdDevModuleNoRowCountHighVariance() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/stddevV1/0/.5/rawdataV1/fakeTimeSeries/333/8555";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(47, rows.size());
	}
	
	@Test
	public void hitStdDevModuleWithRowCountHighVariance() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/stddevV1/100/.5/rawdataV1/fakeTimeSeries/333/8555";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(30, rows.size());
	}
	
	@Test
	public void hitStdDevModuleWithRowCountLargerThanDataSetLowVariance() throws JsonGenerationException, JsonMappingException, IOException {
		//equivilant to hitStdDevModuleNoRowCountLowVariance
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/stddevV1/10000/.1/rawdataV1/fakeTimeSeries/333/8555";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(9, rows.size());
	}
	

	
	@Test
	public void hitRawModuleWithBigIntsFilteredToNull() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/nullV1/rawdataV1/fakeTimeSeriesWithNull/0/16001";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(203, rows.size());
		Assert.assertTrue(StringUtils.contains(theString, "{\"time\":1,\"value\":null"));
	}

	@Test
	public void hitCleanModule() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String requestUri = "/api/rangecleanV1/10/50/rawdataV1/fakeTimeSeries/333/8555";
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Map root = mapper.readValue(theString, Map.class);
		
		List rows = (List) root.get("data");
		Assert.assertEquals(41, rows.size());
	}
	
	@Test
	public void hitSumStreamsMod() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		//FIRST, let's register an aggregation with name=myagg
		RegisterAggregation request = createAggRequest2();
		String json = Parsing.parseJson(request);
		log.warn("SUPEREOIHJGD" + json);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/register", json,  StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String requestUri = "/api/sumstreamsV1/mysum/5000/8000";
		String theString = Utility.sendRequest(httpclient, requestUri,  StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
//		Map map = (Map) root;
//		List object = (List) map.get("data");
//		Assert.assertEquals(6, object.size());
//		
//		Map map1 = (Map) object.get(0);
//		Map map2 = (Map) object.get(1);
//		Double point1 = (Double) map1.get("value");
//		Double point2 = (Double) map2.get("value");
//		Assert.assertTrue(point1.equals(new Double(23.5)));
//		Assert.assertTrue(point2.equals(new Double(23.2)));
		
		String resultBody = Utility.sendRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/aggregation/"+request.getName(), StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		Assert.assertEquals("{\"name\":\"mysum\",\"group\":\"supa\",\"database\":\"supa\",\"urls\":[\"splinesV3(interval=20,epochOffset=5)/rawdataV1/fakeTimeSeries\",\"splinesV3(interval=20,epochOffset=5)/rawdataV1/fakeTimeSeries2\"]}", resultBody);
		
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/deleteagg/"+request.getName(), json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		Utility.sendRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/aggregation/"+request.getName(), StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY, 404);
		
		log.info("class="+root);
	}
	
	
	@Test
	public void hitSumStreamsModHighPrecision() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		//FIRST, let's register an aggregation with name=highprecisionsum
		RegisterAggregation request = createAggRequestHighPrecision();
		String json = Parsing.parseJson(request);
		log.warn("SUPEREOIHJGD" + json);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/register", json,  StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String requestUri = "/api/sumstreamsV1/highprecisionsum/9659/9661";
		String theString = Utility.sendRequest(httpclient, requestUri,  StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();		
		Object root = mapper.readValue(theString, Object.class);
		
		Assert.assertTrue(theString.contains("\"value\":60800.63637866705404"));

		
//		String resultBody = Utility.sendRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/aggregation/"+request.getName(), StartupV2.ROBOT_USER, StartupV2.ROBOT_KEY);
//
//		Assert.assertEquals("{\"name\":\"mysum\",\"group\":\"supa\",\"urls\":[\"splineV1/20/5/rawdataV1/fakeTimeSeries\",\"splineV1/20/5/rawdataV1/fakeTimeSeries2\"],\"_postKey\":null}", resultBody);
//		
//		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/deleteagg/"+request.getName(), json, StartupV2.ROBOT_USER, StartupV2.ROBOT_KEY);
//
//		Utility.sendRequest(httpclient, "http://localhost:" + port + "/api/sumstreamsV1/aggregation/"+request.getName(), StartupV2.ROBOT_USER, StartupV2.ROBOT_KEY, 404);
//		
//		log.info("class="+root);
	}

	private RegisterAggregation createAggRequest2() {
		List<String> urls = new ArrayList<String>();
		urls.add("splinesV3(interval=20,epochOffset=5)/rawdataV1/fakeTimeSeries");
		urls.add("splinesV3(interval=20,epochOffset=5)/rawdataV1/fakeTimeSeries2");
		RegisterAggregation agg = new RegisterAggregation();
		agg.setName("mysum");
		agg.setUrls(urls);
		agg.setGroup("supa");
		return agg;
	}
	
	private RegisterAggregation createAggRequestHighPrecision() {
		List<String> urls = new ArrayList<String>();
		urls.add("rawdataV1/highPrecision1");
		urls.add("rawdataV1/highPrecision2");
		urls.add("rawdataV1/highPrecision3");
		urls.add("rawdataV1/highPrecision4");
		urls.add("rawdataV1/highPrecision5");
		urls.add("rawdataV1/highPrecision6");
		urls.add("rawdataV1/highPrecision7");
		RegisterAggregation agg = new RegisterAggregation();
		agg.setName("highprecisionsum");
		agg.setUrls(urls);
		agg.setGroup("supa");
		return agg;
	}
}
