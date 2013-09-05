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

import models.message.PostTrigger;
import models.message.PostTriggers;
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

public class TestPostTriggerController {

	private int port = Utility.retrievePlayServerPort();

	@Test
	public void testBasicAddDeleteList() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		long r = System.currentTimeMillis();
		String database = StartupDetailed.GROUP1;
		String tableName1 = "triggerTestingTable2"+r;
		String json1 = RegisterPostAndGet.createJsonForRequest(tableName1, true);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json1, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String createTrigger1 = createPostTrigger(tableName1, StartupDetailed.GROUP1);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/posttriggerV1/add", createTrigger1, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String requestUri = "/api/posttriggerV1/list/"+database;
		String theString1 = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper1 = new ObjectMapper();
		PostTriggers root1 = mapper1.readValue(theString1, PostTriggers.class);
		
		Assert.assertEquals(1, root1.getTriggers().size());

		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/api/posttriggerV1/delete/"+tableName1, "hghg", StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		String theString = Utility.sendRequest(httpclient, requestUri, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);

		ObjectMapper mapper = new ObjectMapper();
		PostTriggers root = mapper.readValue(theString, PostTriggers.class);
		
		Assert.assertEquals(0, root.getTriggers().size());		
	}

	private String createPostTrigger(String tableName, String db) throws JsonGenerationException, JsonMappingException, IOException {
		PostTrigger t = new PostTrigger();
		t.setDatabase(db);
		t.setTable(tableName);
		t.setScript("myscript");
		t.setScriptLanguage("javascript");
		
		ObjectMapper mapper = new ObjectMapper();
		StringWriter out = new StringWriter();
		mapper.writeValue(out, t);
		String json = out.toString();
		
		return json;
	}

}
