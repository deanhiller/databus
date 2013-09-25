package gov.nrel.util;

import org.playorm.cron.api.CronListener;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;

public class AMonitorListener implements CronListener {

	private static final Logger log = LoggerFactory.getLogger(AMonitorListener.class);
	private ATableMonitorListener monitorListener;
	private ACronJobListener cronJobListener;
	private NoSqlEntityManagerFactory factory;
	
	public AMonitorListener(NoSqlEntityManagerFactory factory) {
		this.factory = factory;
		this.monitorListener = new ATableMonitorListener();
		CronService svc = CronServiceFactory.getSingleton(null);
		this.cronJobListener = new ACronJobListener(svc);
	}

	@Override
	public void monitorFired(PlayOrmCronJob m) {
		if(log.isInfoEnabled())
			log.info("firing monitor="+m.getId()+" type="+m.getType());
		NoSqlEntityManager mgr = factory.createEntityManager();
		if("trigger".equals(m.getType()) || "cronjob".equals(m.getType())) {
			m.setType("cronjob");
			cronJobListener.monitorFired(m);
		} else
			monitorListener.monitorFired(mgr, m);
	}

}
