package gov.nrel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.Row;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

public abstract class TransferSuper {

	private static final Logger log = LoggerFactory.getLogger(TransferSuper.class);

	protected abstract void transfer();
	
	protected NoSqlEntityManager initialize() {
		String keyspace = (String) Play.configuration.get("transfer.nosql.cassandra.keyspace");
		String cluster = (String) Play.configuration.get("transfer.nosql.clusterName");
		String seeds = (String) Play.configuration.get("transfer.nosql.seeds");
		String port = (String) Play.configuration.get("transfer.nosql.port");

        List<Class> classes = Play.classloader.getAnnotatedClasses(NoSqlEntity.class);
        List<Class> classEmbeddables = Play.classloader.getAnnotatedClasses(NoSqlEmbeddable.class);
        classes.addAll(classEmbeddables);
        
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(Bootstrap.TYPE, "cassandra");
		props.put(Bootstrap.CASSANDRA_KEYSPACE, keyspace);
		props.put(Bootstrap.CASSANDRA_CLUSTERNAME, cluster);
		props.put(Bootstrap.CASSANDRA_SEEDS, seeds);
		props.put(Bootstrap.CASSANDRA_THRIFT_PORT, port);
		props.put(Bootstrap.AUTO_CREATE_KEY, "create");
		props.put(Bootstrap.LIST_OF_EXTRA_CLASSES_TO_SCAN_KEY, classes);

		NoSqlEntityManagerFactory factory2 = Bootstrap.create(props, Play.classloader);
		NoSqlEntityManager mgr2 = factory2.createEntityManager();
		return mgr2;
	}
	
	protected void portTableToNewCassandra(NoSqlEntityManager mgr,
			NoSqlEntityManager mgr2, String cf) {
		log.info("starting port of Column Family="+cf);
		NoSqlTypedSession session = mgr.getTypedSession();
		NoSqlTypedSession session2 = mgr2.getTypedSession();
		NoSqlSession raw = session.getRawSession();
		NoSqlSession raw2 = session2.getRawSession();
		
		DboTableMeta meta = mgr.find(DboTableMeta.class, cf);
		DboTableMeta meta2 = mgr2.find(DboTableMeta.class, cf);
		AbstractCursor<Row> rows = raw.allRows(meta, 500);
		
		int counter = 0;
		while(rows.next()) {
			Row row = rows.getCurrent();
			List<com.alvazan.orm.api.z8spi.action.Column> columns = new ArrayList<com.alvazan.orm.api.z8spi.action.Column>();
			for(com.alvazan.orm.api.z8spi.action.Column c : row.getColumns()) {
				c.setTimestamp(null);
			}
			columns.addAll(row.getColumns());
			raw2.put(meta2, row.getKey(), columns);

//debug code to see values of bytes easier...			
//			ByteArray b = new ByteArray(row.getKey());
//			int d = 5+6;
//			for(com.alvazan.orm.api.z8spi.action.Column c : columns) {
//				ByteArray name = new ByteArray(c.getName());
//				ByteArray col = new ByteArray(c.getValue());
//				int a = 5+4;
//			}
			
			counter++;
			if(counter % 50 == 0) {
				raw.clear(); //clear read cache
				raw2.flush();
			}

			if(counter % 300 == 0) {
				log.info("ported "+counter+" records for cf="+cf+" and still porting");
			}
		}
		
		raw.clear(); //clear read cache
		raw2.flush();
		log.info("done porting. count="+counter+" records for cf="+cf);
	}
}
