package gov.nrel.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import models.AppProperty;
import models.EntityUser;

import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.slf4j.LoggerFactory;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import com.alvazan.orm.api.base.DbTypeEnum;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

@OnApplicationStart
public class StartupBean extends Job {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(StartupBean.class);
	private static CronService svc;

	@Override
	public void doJob() {
		try {
			doJobImpl();
		} catch (Exception e) {
			if (log.isWarnEnabled())
        		log.warn("Exception.  Running System.exit since playframework does not stop on startup exceptions", e);
			Runnable r = new Runnable() {
				@Override
				public void run() {
					System.exit(-1);
				}
			};
			Executors.newFixedThreadPool(1).execute(r);
		}
	}

	public void doJobImpl() throws Exception {
		String prop = Play.configuration.getProperty("nosql.db");
		DbTypeEnum db = DbTypeEnum.IN_MEMORY;
		if("cassandra".equalsIgnoreCase(prop)) {
			db = DbTypeEnum.CASSANDRA;
		}

		DboTableMeta tm = NoSql.em().find(DboTableMeta.class, "partitions");
		if(tm == null) {
			tm = new DboTableMeta();
			tm.setup(null, "partitions", false, null);
			tm.setColNameType(long.class);
			DboColumnIdMeta idMeta = new DboColumnIdMeta();
			idMeta.setup(tm, "table", String.class, false);

			NoSql.em().put(idMeta);
			NoSql.em().put(tm);
			NoSql.em().flush();
		}

		//This method only runs in development mode!!!!...
		runDevelopmentStuff(db);
		
		startMonitorService();
		
		Upgrade8Bean up = new Upgrade8Bean();
		//up.readOnlyTest();
		up.upgrade();

		TransferBean b = new TransferBean();
		b.transfer();
		
		TransferBean2 b2 = new TransferBean2();
		b2.transfer();
	}
	
	private void startMonitorService() {
		NoSqlEntityManagerFactory factory = NoSql.getEntityManagerFactory();
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(CronServiceFactory.NOSQL_MGR_FACTORY, factory);
		String host = UniqueKeyGenerator.getHostname();
		props.put(CronServiceFactory.HOST_UNIQUE_NAME, host);
		String time = Play.configuration.getProperty("monitor.rate");
		props.put(CronServiceFactory.SCAN_RATE_MILLIS, ""+time);
		
		svc = CronServiceFactory.getSingleton(props);
		svc.addListener(new AMonitorListener(factory));

		String emailHost = Play.configuration.getProperty("email.host");
		if(emailHost != null) {
			if (log.isInfoEnabled())
				log.info("starting monitor service mailing to host="+emailHost+" rate="+time);
			svc.start();
		} else {
			if (log.isInfoEnabled())
				log.info("monitor service will NOT be started since no email.host property in application.conf found");
		}
	}

	private void runDevelopmentStuff(DbTypeEnum db) {
		String mode = Play.configuration.getProperty("application.mode");
		String demomode = Play.configuration.getProperty("demo.mode");
		if("prod".equals(mode) && !"true".equals(demomode)) {
			if (log.isInfoEnabled())
				log.info("running in production so skipping the rest of startup bean that sets up a mock database");
			return;
		}		
		if (log.isInfoEnabled())
			log.info("running in dev mode, now checking if we need to initialize the database of type="+db);
		
		NoSqlEntityManager em = NoSql.em();
		
		if(EntityUser.findAll(em).size() > 0)
			return; //There is already data so we don't need to populate duplicate data
		
		if (log.isInfoEnabled())
			log.info("initializing database since main user does not exist");

		//for upgrade beans that should be set to -1 (ie. they don't run in production yet
		AppProperty property = new AppProperty();
		property.setName("schema.version");
		property.setValue(-1+"");
		NoSql.em().put(property);
		NoSql.em().flush();
		
		StartupDetailed.createStuff();
		StartupGroups.createStuff();
		StartupRelational.createStuff();
		StartupForAggregations.setup();
	}
	
}
