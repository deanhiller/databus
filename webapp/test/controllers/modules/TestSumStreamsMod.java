package controllers.modules;

import gov.nrel.util.StartupForAggregations;
import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.EntityUser;
import models.message.RegisterAggregation;

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

import robot.ModuleTests;
import robot.Utility;

public class TestSumStreamsMod {
	private final static Logger log = LoggerFactory.getLogger(TestSumStreamsMod.class);
	
	private static int port = Utility.retrievePlayServerPort();

	@Test
	public void testListAggregations() throws JsonGenerationException, JsonMappingException, IOException {
		/**
		 *  This test class is dependent on the app/gov/nrel/util/StartupBean kicking off StartupForAggregations.setup().
		 *  All users, groups, databases, tables and data are created in there.  
		 */
		
		/*
		 *  Create the aggregations
		 */
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		// Aggregation 1
		List<String> agg1_urls = new ArrayList<String>();
		agg1_urls.add("splineV1/20/5/rawdataV1/aggTable1");
		agg1_urls.add("splineV1/20/5/rawdataV1/aggTable2");
		agg1_urls.add("splineV1/20/5/rawdataV1/aggTable3");
		RegisterAggregation agg1 = new RegisterAggregation();
		agg1.setName("agg1_test");
		agg1.setUrls(agg1_urls);
		agg1.setDatabase("agg1Db");
		agg1.setGroup("agg1Db");
		registerAggregations(httpclient, agg1, StartupForAggregations.AGGUSER1);
		
		// Aggregation 2
		List<String> agg2_urls = new ArrayList<String>();
		agg2_urls.add("splineV1/20/5/rawdataV1/aggTable2");
		agg2_urls.add("splineV1/20/5/rawdataV1/aggTable7");
		agg2_urls.add("splineV1/20/5/rawdataV1/aggTable8");
		RegisterAggregation agg2 = new RegisterAggregation();
		agg2.setName("agg2_test");
		agg2.setUrls(agg2_urls);
		agg2.setDatabase("agg1Db");
		agg2.setGroup("agg1Db");
		registerAggregations(httpclient, agg2, StartupForAggregations.AGGUSER1);
		
		// Aggregation 3
		List<String> agg3_urls = new ArrayList<String>();
		agg3_urls.add("splineV1/20/5/rawdataV1/aggTable1");
		agg1_urls.add("splineV1/20/5/rawdataV1/aggTable3");
		agg3_urls.add("splineV1/20/5/rawdataV1/aggTable9");
		RegisterAggregation agg3 = new RegisterAggregation();
		agg3.setName("agg3_test");
		agg3.setUrls(agg3_urls);
		agg3.setDatabase("agg1Db");
		agg3.setGroup("agg1Db");
		registerAggregations(httpclient, agg3, StartupForAggregations.AGGUSER1);	
		
		// Aggregation 4
		List<String> agg4_urls = new ArrayList<String>();
		agg4_urls.add("splineV1/20/5/rawdataV1/aggTable4");
		agg4_urls.add("splineV1/20/5/rawdataV1/aggTable5");
		agg4_urls.add("splineV1/20/5/rawdataV1/aggTable6");
		RegisterAggregation agg4 = new RegisterAggregation();
		agg4.setName("agg4_test");
		agg4.setUrls(agg4_urls);
		agg4.setDatabase("agg2Db");
		agg4.setGroup("agg2Db");
		registerAggregations(httpclient, agg4, StartupForAggregations.AGGUSER2);
		
		// Aggregation 5
		List<String> agg5_urls = new ArrayList<String>();
		agg5_urls.add("splineV1/20/5/rawdataV1/aggTable5");
		agg5_urls.add("splineV1/20/5/rawdataV1/aggTable7");
		agg5_urls.add("splineV1/20/5/rawdataV1/aggTable9");
		RegisterAggregation agg5 = new RegisterAggregation();
		agg5.setName("agg5_test");
		agg5.setUrls(agg5_urls);
		agg5.setDatabase("agg2Db");
		agg5.setGroup("agg2Db");
		registerAggregations(httpclient, agg5, StartupForAggregations.AGGUSER2);
		
		// Aggregation 6
		List<String> agg6_urls = new ArrayList<String>();
		agg6_urls.add("splineV1/20/5/rawdataV1/aggTable4");
		agg6_urls.add("splineV1/20/5/rawdataV1/aggTable6");
		agg6_urls.add("splineV1/20/5/rawdataV1/aggTable8");
		RegisterAggregation agg6 = new RegisterAggregation();
		agg6.setName("agg6_test");
		agg6.setUrls(agg6_urls);
		agg6.setDatabase("agg2Db");
		agg6.setGroup("agg2Db");
		registerAggregations(httpclient, agg6, StartupForAggregations.AGGUSER2);
		
		// Aggregation 7
		List<String> agg7_urls = new ArrayList<String>();
		agg7_urls.add("splineV1/20/5/rawdataV1/aggTable7");
		agg7_urls.add("splineV1/20/5/rawdataV1/aggTable8");
		agg7_urls.add("splineV1/20/5/rawdataV1/aggTable9");
		RegisterAggregation agg7 = new RegisterAggregation();
		agg7.setName("agg7_test");
		agg7.setUrls(agg7_urls);
		agg7.setDatabase("aggGroupDb");
		agg7.setGroup("aggGroupDb");
		registerAggregations(httpclient, agg7, StartupForAggregations.AGGUSER1);	
		
		// Now test User 1's aggregations
		// It should NOT see aggregations 4, 5 and 6
		String request1Uri = "/api/sumstreamsV1/listaggregations/";
		String result1Body = Utility.sendRequest(httpclient, request1Uri, StartupForAggregations.AGGUSER1.getName(), StartupForAggregations.AGGUSER1.getApiKey());
		
		Assert.assertEquals("{\"aggregations\":[{\"database\":\"agg1Db\",\"group\":\"agg1Db\",\"aggregation\":[\"agg1_test\",\"agg2_test\",\"agg3_test\"]},{\"database\":\"aggGroupDb\",\"group\":\"aggGroupDb\",\"aggregation\":[\"agg7_test\"]}]}", result1Body);
		
		
		// Now test User 2's aggregations
		// It should NOT see aggregations 1, 2, and 3
		String request2Uri = "/api/sumstreamsV1/listaggregations/";
		String result2Body = Utility.sendRequest(httpclient, request2Uri, StartupForAggregations.AGGUSER2.getName(), StartupForAggregations.AGGUSER2.getApiKey());
		
		Assert.assertEquals("{\"aggregations\":[{\"database\":\"agg2Db\",\"group\":\"agg2Db\",\"aggregation\":[\"agg4_test\",\"agg5_test\",\"agg6_test\"]},{\"database\":\"aggGroupDb\",\"group\":\"aggGroupDb\",\"aggregation\":[\"agg7_test\"]}]}", result2Body);
	}
	
	private static void registerAggregations(DefaultHttpClient httpclient, RegisterAggregation request, EntityUser user) {
		String json = Parsing.parseJson(request);
		log.warn("StartupForAggregations.registerAggregations() POST CONTENT: " + json);
		
		String url = "http://localhost:" + port + "/api/sumstreamsV1/register";
		log.warn("StartupForAggregations.registerAggregations() URL: " + url);
		Utility.sendPostRequest(httpclient, url, json,  user.getName(), user.getApiKey());
	}
}
