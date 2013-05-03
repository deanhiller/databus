package modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.play.NoSql;
import com.alvazan.play.logging.CassandraAppender;

import play.Play;
import play.PlayPlugin;

public class NoSqlPlugin extends PlayPlugin {

	private static final Logger log = LoggerFactory.getLogger(NoSqlPlugin.class);

	@Override
	public void onApplicationStop() {
		if (log.isInfoEnabled())
			log.info("nosql plugin stopping");
//		super.onApplicationStop();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void onApplicationStart() {
		if (log.isInfoEnabled())
			log.info("nosql plugin starting");
//        List<Class> classes = Play.classloader.getAnnotatedClasses(NoSqlEntity.class);
//        if (classes.isEmpty())
//            return;
//        else if (NoSql.getEntityManagerFactory() != null) {
//        	NoSqlEntityManagerFactory factory = NoSql.getEntityManagerFactory();
//        	factory.rescan(classes, Play.classloader);
//        	return;
//        }
//        
//        Map<String, Object> props = new HashMap<String, Object>();
//        props.put(Bootstrap.LIST_OF_EXTRA_CLASSES_TO_SCAN_KEY, classes);
//        props.put(Bootstrap.AUTO_CREATE_KEY, "create");
//        
//        for(java.util.Map.Entry<Object, Object> entry : Play.configuration.entrySet()) {
//        	props.put((String) entry.getKey(), entry.getValue());
//        }
//        
//        log.info("Initializing PlayORM...");
//
//        NoSqlEntityManagerFactory factory = Bootstrap.create(props, Play.classloader);
//        NoSql.setEntityManagerFactory(factory);
//        CassandraAppender.setFactory(factory);
	}


}
