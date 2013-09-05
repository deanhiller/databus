package controllers.api;

import gov.nrel.util.AMonitorListener;
import gov.nrel.util.ATriggerListener;
import gov.nrel.util.StartupDetailed;
import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.message.RegisterResponseMessage;
import models.message.Trigger;
import models.message.Triggers;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;

import play.Play;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.play.NoSql;

import robot.RegisterPostAndGet;
import robot.Utility;

public class TestCronJobController {

	private int port = Utility.retrievePlayServerPort();

	@Test
	public void testBasicAddDeleteList() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String database = StartupDetailed.GROUP1;
		String tableName1 = "logTestingTable2"+r;
		String json1 = RegisterPostAndGet.createJsonForRequest(tableName1, true);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json1, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		String tableName2 = "logTestingTable2"+(r+1);
		String reg2 = RegisterPostAndGet.createJsonForRequest(tableName2, true);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", reg2, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String createTrigger1 = createTrigger(tableName1, StartupDetailed.GROUP1);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/triggerV1/add", createTrigger1, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		String createTrigger2 = createTrigger(tableName2, StartupDetailed.GROUP1);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/triggerV1/add", createTrigger2, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String requestUri = "/api/triggerV1/list/"+database;
		String theString1 = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper1 = new ObjectMapper();
		Triggers root1 = mapper1.readValue(theString1, Triggers.class);
		
		Assert.assertEquals(2, root1.getTriggers().size());
		
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/triggerV1/delete/"+tableName1, reg2, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/triggerV1/delete/"+tableName2, reg2, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper = new ObjectMapper();
		Triggers root = mapper.readValue(theString, Triggers.class);
		
		Assert.assertEquals(0, root.getTriggers().size());		
	}

	private String createTrigger(String tableName, String db) throws JsonGenerationException, JsonMappingException, IOException {
		Trigger t = new Trigger();
		t.setDatabase(db);
		t.setAfter(2000);
		t.setBefore(2000);
		t.setId(tableName);
		t.setOffset(0);
		long oneHour = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		t.setRate(oneHour);
		t.setUrl("http://localhost:"+port+"/api/logV1/"+tableName+"/splinesV2/basic/10/3/invertV1/rawdataV1/fakeTimeSeries");
		
		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, t);
		String json = out.toString();
		
		return json;
	}

	@Test
	public void testFiringTrigger() throws JsonGenerationException, JsonMappingException, IOException {
		long r = System.currentTimeMillis();
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String tableName1 = "logTestTable2"+r;
		String json1 = RegisterPostAndGet.createJsonForRequest(tableName1, true);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json1, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		CronService mockSvc = new MockCronSvc();
		ATriggerListener l = new ATriggerListener(mockSvc, 3400, 4300);
		PlayOrmCronJob job = new PlayOrmCronJob();
		job.setId("_logMyLoggingTable");
		job.setEpochOffset(30L);
		job.setTimePeriodMillis(50);
		job.setType("trigger");
		job.getProperties().put("offset", ""+job.getEpochOffset());
		job.getProperties().put("after", "20");
		job.getProperties().put("before", "20");
		job.getProperties().put("userName", StartupGroups.ROBOT_USER);
		job.getProperties().put("apiKey", StartupGroups.ROBOT_KEY);
		job.getProperties().put("schemaId", null);
		job.getProperties().put("url", "http://localhost:"+port+"/api/logV1/"+tableName1+"/splinesV2/basic/10/3/invertV1/rawdataV1/fakeTimeSeries");

		l.monitorFired(job);
		
		
	}
}
