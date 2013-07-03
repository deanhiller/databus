package gov.nrel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import play.Play;

import models.EntityUser;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;
import controllers.api.ApiRegistrationImpl;

public class StartupDetailed {

	//these are actually "schemas"/"databases", not groups anymore:
	public static final String GROUP1 = "supa";
	public static final String GROUP2 = "tempDean";
	
	private static final String TS_RELATIONAL = "wideTable";
	private static final String FAKE_TIME_SERIES = "fakeTimeSeries";
	private static final String FAKE_TIME_SERIES2 = "fakeTimeSeries2";
	private static final String FAKE_TIME_SERIES_WITH_NULL = "fakeTimeSeriesWithNull";
	private static final String HIGH_PRECISION_TIME_SERIES = "highPrecision";
	private static final String SPLINE_TESTS = "splineTests";
	static final EntityUser ADMIN;
	public static final EntityUser DEAN;
	static final EntityUser JUSTIN;
	
	static {
		DEAN = new EntityUser();
		DEAN.setApiKey("someuniquekeyforregistration");
		DEAN.setUsername("dhiller2");
		DEAN.setPassword("nreliscool");
		
		JUSTIN = new EntityUser();
		JUSTIN.setApiKey("justinsunhackablekey");
		JUSTIN.setUsername("jcollins");
		JUSTIN.setPassword("nreliscool");
		
		ADMIN = new EntityUser();
		ADMIN.setAdmin(true);
		ADMIN.setApiKey("adminregkey");
		ADMIN.setUsername("admin");
		ADMIN.setPassword("nreliscool");
	}
	
	public static void createStuff() {

		NoSql.em().put(DEAN);
		NoSql.em().put(JUSTIN);
		NoSql.em().put(ADMIN);
		
		NoSql.em().flush();
		
		StartupUtils.createDatabase(NoSql.em(), DEAN, GROUP1, JUSTIN);
		StartupUtils.createDatabase(NoSql.em(), DEAN, GROUP2, JUSTIN);
		
		
		NoSql.em().flush();
		
		//Demo Data:
		String mode = Play.configuration.getProperty("demo.mode");
		if("true".equals(mode)) 
		{
			StartupDemo.createStuff();
		}
		

		createTsRelational(TS_RELATIONAL, StartupDetailed.GROUP1, DEAN);
		
		//do time series AFTER the flush above so security will check out ok
		createSeriesImplWithUnitTestData(FAKE_TIME_SERIES, 25, 30, StartupDetailed.GROUP1, DEAN);
		createSeriesImplWithUnitTestData2(FAKE_TIME_SERIES2, 55, 60, StartupDetailed.GROUP1, DEAN);
		createSeriesImplWithUnitTestData(FAKE_TIME_SERIES_WITH_NULL, 55, 60, StartupDetailed.GROUP1, DEAN);
		//one at the beginning, one in the middle, one at the end to test all cases:
		putData(FAKE_TIME_SERIES_WITH_NULL, ""+1, ""+Integer.MAX_VALUE, DEAN);
		putData(FAKE_TIME_SERIES_WITH_NULL, ""+9660, ""+Integer.MAX_VALUE, DEAN);
		putData(FAKE_TIME_SERIES_WITH_NULL, ""+16000, ""+Integer.MAX_VALUE, DEAN);

		
		//This is the real data from a failure case for sumstreams.
		//See ModuleTests.hitSumStreamsModHighPrecision()
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+1, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+1, ""+9660, "6221.513944824521", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+2, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+2, ""+9660, "27064.499539798995", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+3, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+3, ""+9660, "485.47567852982854", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+4, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+4, ""+9660, "1776.051897058598", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+5, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+5, ""+9660, "22525.384206866485", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+6, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+6, ""+9660, "1430.9779126497479", DEAN);
		createSeriesImplWithUnitTestData(HIGH_PRECISION_TIME_SERIES+7, 55, 60, StartupDetailed.GROUP1, DEAN);
		putData(HIGH_PRECISION_TIME_SERIES+7, ""+9660, "1296.7331989388786", DEAN);
		
		
		//This is for very basic spline checking
		createEmpty(SPLINE_TESTS+1);
		putData(SPLINE_TESTS+1, ""+100, "100", DEAN);
		putData(SPLINE_TESTS+1, ""+200, "25", DEAN);
		putData(SPLINE_TESTS+1, ""+300, "50", DEAN);
		putData(SPLINE_TESTS+1, ""+400, "0", DEAN);		

		NoSql.em().flush();
	}
	
	private static void createEmpty(String name) {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		List<String> groups = new ArrayList<String>();
		groups.add(StartupDetailed.GROUP1);
		msg.setGroups(groups);
		msg.setModelName(name);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		StartupUtils.createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		StartupUtils.createColumn(cols, "value", "BigDecimal", "oei:temparaturecolor", false, false);
		msg.setColumns(cols);	

		//register the time series table we will use
		RegisterResponseMessage resp = ApiRegistrationImpl.registerImpl(msg, DEAN.getUsername(), DEAN.getApiKey());

	}
	
	static void createSeriesImplWithUnitTestData(String name, int startInterval, int endInterval, String schemaName, EntityUser user) {
		createSeriesImpl(name, schemaName, user, DatasetType.STREAM);
		insertUnitTestData(name, startInterval, endInterval, user);
	}
	
	static void createSeriesImplWithUnitTestData2(String name, int startInterval, int endInterval, String schemaName, EntityUser user) {
		createSeriesImpl(name, schemaName, user, DatasetType.TIME_SERIES);
		insertUnitTestData(name, startInterval, endInterval, user);
	}	
	
	static void createSeriesImpl(String name, String schemaName, EntityUser user, DatasetType type) {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(type);
		msg.setSchema(schemaName);
		msg.setModelName(name);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		StartupUtils.createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		StartupUtils.createColumn(cols, "value", "BigDecimal", "oei:temparaturecolor", false, false);
		msg.setColumns(cols);	

		//register the time series table we will use
		RegisterResponseMessage resp = ApiRegistrationImpl.registerImpl(msg, user.getUsername(), user.getApiKey());
	}
	
	static void insertUnitTestData(String name, int startInterval, int endInterval, EntityUser user) {
		int start = 3456;
		long time = 0;
		for(int counter = 0; counter < 10; counter++) {
			time = start + counter*startInterval;
			putData(name, time+"", counter+"", user);
		}
		
		long nextStart = time;
		for(int counter = 10; counter < 200; counter++) {
			long t2 = nextStart + counter*endInterval;
			putData(name, t2+"", counter+"", user);
		}
	}
	
	

	static void putData(String tableName, String time, String value, EntityUser user) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put("_tableName", tableName);
		json.put("time", time);
		json.put("value", value);
		ApiPostDataPointsImpl.postDataImpl(null, json, user.getUsername(), user.getApiKey());
	}

	static void createTsRelational(String name, String schemaName, EntityUser user) {
		createWideTable(name, schemaName, user);
		insertWideData(name, user);
	}
	
	static void createWideTable(String name, String schemaName, EntityUser user) {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.RELATIONAL_TABLE);
		msg.setSchema(schemaName);
		msg.setModelName(name);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		StartupUtils.createColumn(cols, "timeStart", "BigInteger", "oei:timestamp", true, true);
		StartupUtils.createColumn(cols, "temp", "BigDecimal", "oei:temparaturecolor", false, false);
		StartupUtils.createColumn(cols, "volume", "BigDecimal", "oei:temparaturecolor", false, false);
		StartupUtils.createColumn(cols, "energy", "BigDecimal", "oei:temparaturecolor", false, false);
		StartupUtils.createColumn(cols, "number", "BigDecimal", "oei:temparaturecolor", false, false);
		msg.setColumns(cols);	

		//register the time series table we will use
		RegisterResponseMessage resp = ApiRegistrationImpl.registerImpl(msg, user.getUsername(), user.getApiKey());
	}
	
	static void insertWideData(String name, EntityUser user) {
		int totalPoints = 100;
		long start = 1367846618000L;
		long hourInMillis = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long end = totalPoints*hourInMillis+start;

		for(long time = start,n = 0; time < end; time+=hourInMillis, n++) {
			String temp = n+"";
			String volume = (totalPoints-n) +"";
			String energy = Math.pow(n-(totalPoints/2), 2)+"";
			//here, let's shift the power graph over to center it...
//			double divisor = Math.pow(totalPoints/2, 2)/totalPoints;
//			String energy = (Math.pow(n-(totalPoints/2), 2) / divisor)+"";
			String number = totalPoints/2+"";
			putWideData(name, user, time+"", temp, volume, energy, number);
		}
	}
	
	static void putWideData(String tableName, EntityUser user, String time, String temp, String volume, String energy, String number) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put("_tableName", tableName);
		json.put("timeStart", time);
		json.put("temp", temp);
		json.put("volume", volume);
		json.put("energy", energy);
		json.put("number", number);
		ApiPostDataPointsImpl.postDataImpl(null, json, user.getUsername(), user.getApiKey());
	}
}
