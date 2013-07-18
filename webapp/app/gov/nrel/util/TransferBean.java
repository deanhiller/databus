package gov.nrel.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;

import models.SecureSchema;
import models.SecureTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.DbTypeEnum;
import com.alvazan.orm.api.base.Indexing;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.NoSqlRawSession;
import com.alvazan.orm.api.z8spi.Row;
import com.alvazan.orm.api.z8spi.conv.ByteArray;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import play.Play;

public class TransferBean {

	private static final Logger log = LoggerFactory.getLogger(TransferBean.class);

	public void transfer() {
		String upgradeMode = (String) Play.configuration.get("upgrade.mode");
		if(upgradeMode == null || !"TRANSFER".equals(upgradeMode))
			return; //we dont' run unless we are in transfer mode

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
		NoSqlEntityManager mgr = NoSql.em();

		String cf = "User";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "KeyToTableName";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "AppProperty";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "EntityGroupXref";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "SdiColumn";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "SdiTable";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "SecureResourceGroupXref";
		portTableToNewCassandra(mgr, mgr2, cf);

		portOverCursorToMany(mgr, mgr2);

		//time to port indexes now...
		buildIndexesOnNewSystem(mgr, mgr2);

	}

	private void buildIndexesOnNewSystem(NoSqlEntityManager mgr, NoSqlEntityManager mgr2) {
		String cf = "User";
		buildIndexForCf(mgr, mgr2, cf);
		cf = "AppProperty";
		buildIndexForCf(mgr, mgr2, cf);
		cf = "SdiTable";
		buildIndexForCf(mgr, mgr2, cf);
	}

	private void buildIndexForCf(NoSqlEntityManager mgr, NoSqlEntityManager mgr2, String cf) {
		log.info("building index for CF="+cf);
		Indexing.setForcedIndexing(true);
		Cursor<Object> rows = mgr2.allRows(Object.class, cf, 500);
		int counter = 0;
		while(rows.next()) {
			Object obj = rows.getCurrent();
			mgr2.put(obj);
			
			counter++;
			if(counter % 50 == 0) {
				mgr2.clear();
				mgr2.flush();
			}

			if(counter % 300 == 0) {
				log.info("ported "+counter+" records for cf="+cf+" and still porting");
			}
		}
		
		mgr2.clear();
		mgr2.flush();

		Indexing.setForcedIndexing(false);
	}

	private void portOverCursorToMany(NoSqlEntityManager mgr, NoSqlEntityManager mgr2) {
		log.info("porting over all the CursorToMany of SecureTable entities");
		List<SecureSchema> schemas = SecureSchema.findAll(mgr);
		List<SecureSchema> schemas2 = SecureSchema.findAll(mgr2);
		
		Map<String, SecureSchema> map = new HashMap<String, SecureSchema>();
		for(SecureSchema s : schemas) {
			map.put(s.getName(), s);
		}
		
		int counter = 0;
		//form the row key for CursorToMany
		for(SecureSchema s: schemas2) {
			log.info("porting over database="+s.getName()+" id="+s.getId());
			SecureSchema oldSchema = map.get(s.getName());
			if(oldSchema == null)
				throw new RuntimeException("bug, all the same schemas should exist in both systems. s="+s.getName()+" does not exist. id="+s.getId());

			int tableCount = 0;
			CursorToMany<SecureTable> tables = oldSchema.getTablesCursor();
			while(tables.next()) {
				SecureTable table = tables.getCurrent();
				SecureTable tb = mgr2.find(SecureTable.class, table.getId());
				if(tb == null)
					throw new RuntimeException("Table="+table.getName()+" does not exist for some reason.  id looked up="+table.getId());

				s.getTablesCursor().addElement(tb);
				tableCount++;
				counter++;
				if(counter % 50 == 0) {
					mgr2.put(s);
					mgr.clear();
					mgr2.flush();
					log.info("For db="+s.getName()+" ported over table count="+tableCount);
				}
			}

			mgr2.put(s);

		}
		
		mgr.clear();
		mgr2.flush();
	}

	private void portTableToNewCassandra(NoSqlEntityManager mgr,
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
