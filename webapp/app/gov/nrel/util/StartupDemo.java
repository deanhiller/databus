package gov.nrel.util;

import java.util.List;
import java.util.Random;

import org.joda.time.DateTime;

import com.alvazan.play.NoSql;

import models.EntityUser;
import models.SecureSchema;
import models.StreamAggregation;

public class StartupDemo {
	
	public static final String GROUPDEMO = "DemoDatabase";
	
	private static final String DEMO_TIME_SOLAR1 = "SolarPVArray1SouthRoofTopOutput";
	private static final String DEMO_TIME_SOLAR2 = "SolarPVArray2ExteriorOutput";
	private static final String DEMO_TIME_SOLAR3 = "SolarPVArray3GarageOutput";
	private static final String DEMO_TIME_SOLAR4 = "SolarPVArray4SouthSideOutput";
	private static final String DEMO_TIME_SOLAR5 = "SolarPVArray5RoofTopOutput";
	
	
	private static final String DEMO_TIME_SERIESMECH2 = "BuildingMechanical2";
	private static final String DEMO_TIME_SERIESMECH3 = "BuildingMechanicalMeter3";
	private static final String DEMO_TIME_SERIESMECH4 = "BuildingMechanicalElevator4";
	private static final String DEMO_TIME_SERIESMECH5 = "BuildingMechanicalFloor5UAV5";
	private static final String DEMO_TIME_SERIESMECH6 = "BuildingMechanicalFloor6AHU6";
	private static final String DEMO_TIME_SERIESMECH7 = "BuildingMechanicalFloor7AHU7";
	
	private static final String DEMO_TIME_SERIESHEAT2 = "HeatingFloor1";
	private static final String DEMO_TIME_SERIESHEAT3 = "HeatingFloor2";
	private static final String DEMO_TIME_SERIESHEAT4 = "HeatingFloor3";
	private static final String DEMO_TIME_SERIESHEAT5 = "HeatingFloor4";
	private static final String DEMO_TIME_SERIESHEAT6 = "HeatingFloor5";
	private static final String DEMO_TIME_SERIESHEAT7 = "HeatingFloor6";

	
	//2 days forward, 2 days back at 15 minute or hourly intervals:
	private static final long DEMO_DATA_TIME_BACK = 172800000;
	private static final long DEMO_DATA_TIME_FORWARD = 172800000;
	private static final long DEMO_DATA_INTERVAL_15MIN = 900000;
	private static final long DEMO_DATA_INTERVAL_HOURLY = 3600000;
	
	private static final double RANDOMNESS_NONE = 0;
	private static final double RANDOMNESS_VERY_SLIGHT = .1;
	private static final double RANDOMNESS_SLIGHT = .5;
	private static final double RANDOMNESS_SOME = 2;
	private static final double RANDOMNESS_EXTREME = 4;
	
	private static final double FREQUENCY_LOW = .8;
	private static final double FREQUENCY_MED = .5;
	private static final double FREQUENCY_HIGH = .3;
	//private static final long DEMO_DATA_INTERVAL = 90000;
	
	private static final int LEAVE_AS_GENERATED = 0;
	private static final int DROP_BELOW_ZERO = 1;
	private static final int MIRROR_BELOW_ZERO = 2;
	
	private static final String URL_PREFIX_SPLINE = "splinesV1/limitderivative/60000/rawdataV1/";
	
	static final EntityUser DEMO;

	public static long current = 1362145121000L;

	static {
		DEMO = new EntityUser();
		//lets use one that at least looks a little more realistic:
		DEMO.setApiKey("register:15505124196:b1:2361498898283920755");
		DEMO.setUsername("demo");
		DEMO.setPassword("databusiscool");
		
	}
	
	public static void createStuff() {

		NoSql.em().put(DEMO);
		
		NoSql.em().flush();
		
		StartupUtils.createDatabase(NoSql.em(), DEMO, GROUPDEMO, StartupDetailed.JUSTIN);
		
		NoSql.em().flush();
		
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SOLAR1, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_15MIN, GROUPDEMO, DEMO, RANDOMNESS_VERY_SLIGHT, 2000, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SOLAR2, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 600, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SOLAR3, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_VERY_SLIGHT, 500, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SOLAR4, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 600, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SOLAR5, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 1600, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createAggregation("PVSolarArrayAggregation1", GROUPDEMO, 
				URL_PREFIX_SPLINE+DEMO_TIME_SOLAR1, 
				URL_PREFIX_SPLINE+DEMO_TIME_SOLAR2, 
				URL_PREFIX_SPLINE+DEMO_TIME_SOLAR3, 
				URL_PREFIX_SPLINE+DEMO_TIME_SOLAR4, 
				URL_PREFIX_SPLINE+DEMO_TIME_SOLAR5);
		
		
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH2, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_15MIN, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 500, FREQUENCY_LOW, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH3, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_15MIN, GROUPDEMO, DEMO, RANDOMNESS_SOME, 3000, FREQUENCY_LOW, MIRROR_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH4, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_15MIN, GROUPDEMO, DEMO, RANDOMNESS_SOME, 1000, FREQUENCY_HIGH, MIRROR_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH5, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_15MIN, GROUPDEMO, DEMO, RANDOMNESS_SOME, 10000, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH6, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 1600, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESMECH7, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 4000, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createAggregation("BuildingMechanicalAggregation", GROUPDEMO, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH2, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH3, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH4, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH5, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH6,
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESMECH7);
		
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESHEAT2, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SLIGHT, 500, FREQUENCY_HIGH, DROP_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESHEAT3, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SOME, 7000, FREQUENCY_LOW, MIRROR_BELOW_ZERO);
		createSeriesImplWithPrettyDemoData(DEMO_TIME_SERIESHEAT4, DEMO_DATA_TIME_BACK, DEMO_DATA_TIME_FORWARD, DEMO_DATA_INTERVAL_HOURLY, GROUPDEMO, DEMO, RANDOMNESS_SOME, 1000, FREQUENCY_HIGH, MIRROR_BELOW_ZERO);

		createAggregation("BuildingHeatingAggregation", GROUPDEMO, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESHEAT2, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESHEAT3, 
				URL_PREFIX_SPLINE+DEMO_TIME_SERIESHEAT4);


	}
	
	static void createSeriesImplWithPrettyDemoData(String name, long millisBack, long millisForward, long interval, String schemaName, EntityUser user, double randomness, int ampScale, double frequencyScale, int belowZeroHandling) {
		StartupDetailed.createSeriesImpl(name, schemaName, user);
		insertPrettyDemoData(name, current-millisBack, current+millisForward, interval, user, randomness, ampScale, frequencyScale, belowZeroHandling);
	}
	
	public static void insertPrettyDemoData(String name, long startTime, long endTime, long pointFrequency, EntityUser user, double randomness, int ampScale, double frequencyScale, int belowZeroHandling) {
		int FREQ_SCALEFACTOR = 100000;
		Random r = new Random();
		
		for (long i = startTime/1000; i < endTime/1000; i+=pointFrequency/1000) {
			double radians = (Math.PI / (FREQ_SCALEFACTOR*frequencyScale)) * i;
			double result = Math.sin(radians)*ampScale;
			if (randomness != 0)
				result += r.nextDouble()*randomness*ampScale;
			
			if (result < 0 && belowZeroHandling == MIRROR_BELOW_ZERO)
				result = Math.abs(result);
			if (result < 0 && belowZeroHandling == DROP_BELOW_ZERO)
				result = 0;
			StartupDetailed.putData(name, ""+i*1000, ""+result, user);
		}
	}
	
	public static void createAggregation(String aggName, String schemaName, String... urls) {
		StreamAggregation s = NoSql.em().find(StreamAggregation.class, aggName);
		if(s == null) {
			s = new StreamAggregation();
		} else {
			if(!s.getSchema().getName().equals(schemaName))
				throw new RuntimeException("Aggregation name="+aggName+" is already in use under group="+schemaName);
		}

		SecureSchema schema = SecureSchema.findByName(NoSql.em(), schemaName);
		
		s.setName(aggName);
		List<String> streamUrls = s.getUrls();
		streamUrls.clear();
		
		for(String url : urls) {
			streamUrls.add(url);
		}
		
		s.setSchema(schema);
		NoSql.em().put(s);
		NoSql.em().put(schema);
		NoSql.em().flush();
	}

}
