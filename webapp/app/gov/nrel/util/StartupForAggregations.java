package gov.nrel.util;

import java.util.Random;

import models.EntityGroup;
import models.EntityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

public class StartupForAggregations {
	private final static Logger log = LoggerFactory.getLogger(StartupForAggregations.class);
	
	/*
	 *  Create the users and group
	 */
	public static final EntityUser AGGUSER1;
	public static final EntityUser AGGUSER2;
	public static EntityGroup AGGGROUP;
	
	static {
		AGGUSER1 = new EntityUser();
		AGGUSER1.setUsername("aggUser1");
		AGGUSER1.setApiKey("aggUser1Key");
		
		AGGUSER2 = new EntityUser();
		AGGUSER2.setUsername("aggUser2");
		AGGUSER2.setApiKey("aggUser2Key");
	}
	
	public static void setup() {
		/**
		 * 	1) Create 2 users:
		 * 				aggUser1
		 * 				aggUser2
		 *	2) Create 1 group:
		 *				aggGroup
		 *	3) Add aggUser2 to aggGroup
		 *	4) Create 3 Databases:
		 *				database1			- Owned by aggUser1
		 *				database2			- Owned by aggGroup (seen by aggUser1)
		 *				database3			- Owned by aggUser2
		 *	5) Create 9 Tables with Data (3 for each Database)
		 *	6) Create 3 aggregations:
		 *				aggregation1	- within database1
		 *				aggregation2	- within database2
		 *				aggregation3	- within database3
		 *	
		 */
		/*
		 *  Push the users
		 */
		NoSql.em().put(AGGUSER1);
		NoSql.em().put(AGGUSER2);
		
		/*
		 *  Create the group
		 */
		EntityGroup AGGGROUP = StartupUtils.createEntityGroup(NoSql.em(), AGGUSER2, "aggGroup", null);
		
		NoSql.em().flush();
		
		/*
		 *  Create the databases
		 */
		StartupUtils.createDatabase(NoSql.em(), AGGUSER1, "agg1Db", AGGUSER1);
		StartupUtils.createDatabase(NoSql.em(), AGGUSER2, "agg2Db", AGGUSER2);
		StartupUtils.createDatabase(NoSql.em(), AGGUSER1, "aggGroupDb", AGGGROUP);
		
		NoSql.em().flush();
		
		/*
		 *  Create the tables
		 */
		StartupUtils.createTimeSeriesTable("aggTable1", "agg1Db", AGGUSER1);
		StartupUtils.createTimeSeriesTable("aggTable2", "agg1Db", AGGUSER1);
		StartupUtils.createTimeSeriesTable("aggTable3", "agg1Db", AGGUSER1);
		StartupUtils.createTimeSeriesTable("aggTable4", "agg2Db", AGGUSER2);
		StartupUtils.createTimeSeriesTable("aggTable5", "agg2Db", AGGUSER2);
		StartupUtils.createTimeSeriesTable("aggTable6", "agg2Db", AGGUSER2);
		StartupUtils.createTimeSeriesTable("aggTable7", "aggGroupDb", AGGUSER1);
		StartupUtils.createTimeSeriesTable("aggTable8", "aggGroupDb", AGGUSER1);
		StartupUtils.createTimeSeriesTable("aggTable9", "aggGroupDb", AGGUSER1);
		
		NoSql.em().flush();
		
		/*
		 *  Put data in tables
		 */
		createTableData("aggTable1", AGGUSER1); 
		createTableData("aggTable2", AGGUSER1);
		createTableData("aggTable3", AGGUSER1);
		createTableData("aggTable4", AGGUSER2);
		createTableData("aggTable5", AGGUSER2);
		createTableData("aggTable6", AGGUSER2);
		createTableData("aggTable7", AGGUSER1);
		createTableData("aggTable8", AGGUSER1);
		createTableData("aggTable9", AGGUSER1);
		
		NoSql.em().flush();
			
	}
	
	private static void createTableData(String tableName, EntityUser user) {
		Random random = new Random();
		int startInterval =  50;
		int endInterval =  60;
		
		int start = random.nextInt(3456);
		long time = 0;
		for(int counter = 1; counter < 5; counter++) {
			time = start + counter*startInterval;
			StartupUtils.putTimeSeriesData(tableName, time+"", counter+"", user);
		}
		
		long nextStart = time + 1;
		for(int counter = 10; counter < 15; counter++) {
			long t2 = nextStart + counter*endInterval;
			StartupUtils.putTimeSeriesData(tableName, t2+"", counter+"", user);
		}
	}	
}
