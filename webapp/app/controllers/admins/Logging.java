package controllers.admins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;
import com.alvazan.play.logging.LogDao;
import com.alvazan.play.logging.LogEvent;
import com.alvazan.play.logging.ServersThatLog;

import play.mvc.Controller;

public class Logging extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Logging.class);
	
	public static void latestLogs() {
		Cursor<KeyValue<LogEvent>> iterator = LogDao.fetchLatestLogs(NoSql.em(), 300);
		List<LogEvent> logs = new ArrayList<LogEvent>();
		while(iterator.next()) {
			KeyValue<LogEvent> kv = iterator.getCurrent();
			LogEvent event;
			if(kv.getValue() == null) {
				event = new LogEvent();
				event.setId(kv.getKey()+"", 0);
			} else {
				event = kv.getValue();
			}
			logs.add(event);
		}
		render(logs);
	}
	
	public static void allLogs(int rowNum, int size) {
		NoSqlEntityManager em = NoSql.em();
		Cursor<LogEvent> iterator = LogDao.fetchAllLogs(em, 100);
		if (log.isInfoEnabled())
			log.info("user is there");
		List<LogEvent> logs = new ArrayList<LogEvent>();
		int row = 0;
		while(iterator.next()) {
			LogEvent event = iterator.getCurrent();
			row++;
			if(row <= rowNum)
				continue;
			
			logs.add(event);
			if(logs.size() >= size)
				break;
		}
		render(logs);
	}

	public static void sessionLogs(String sessionId) {
		int numDigits = 2;
		List<LogEvent> logs = LogDao.fetchOrderedSessionLogs(NoSql.em(), sessionId, numDigits);
		render(logs);
	}
}
