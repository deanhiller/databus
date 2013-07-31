package gov.nrel.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;

import models.DataTypeEnum;
import models.EntityUser;
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
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import play.Play;

public class TransferBean extends TransferSuper {

	private static final Logger log = LoggerFactory.getLogger(TransferBean.class);

	public void transfer() {
		String upgradeMode = (String) Play.configuration.get("upgrade.mode");
		if(upgradeMode == null || !upgradeMode.startsWith("http")) {
			log.info("NOT RUNNING PORT DATA scripts since upgrademode does not start with http");
			return; //we dont' run unless we are in transfer mode
		}

		NoSqlEntityManager mgr2 = initialize();
		NoSqlEntityManager mgr = NoSql.em();

		//NEXT, we need to check if data has already been ported so we don't port twice
		List<SecureSchema> schemas = SecureSchema.findAll(mgr2);
		if(schemas.size() > 0) {
			log.info("NOT RUNNING PORT DATA scripts to new cassandra since there are existing schemas(this is very bad)");
			return;
		} else
			log.info("RUNNING PORT DATA scripts to new cassandra since no databases found in new cassandra instance");
		
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
		cf = "DboTableMeta";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "DboColumnMeta";
		portTableToNewCassandra(mgr, mgr2, cf);
				
		//time to port indexes now...
		buildIndexesOnNewSystem(mgr, mgr2);

		//Now that we have indexes, we can port cursor to many
		//need indexes first since we use indexes on new cassandra(and use one on old cassandra as well).
		portOverCursorToMany(mgr, mgr2);
		
		cleanupTimeSeriesMeta(mgr2);
		
	}

	private void cleanupTimeSeriesMeta(NoSqlEntityManager mgr2) {
		Set<String> timeSeriesNames = new HashSet<String>();
		timeSeriesNames.add("time");
		timeSeriesNames.add("value");
		Cursor<KeyValue<SecureTable>> cursor = SecureTable.findAllCursor(mgr2);
		int counter = 0;
		while(cursor.next()) {
			KeyValue<SecureTable> kv = cursor.getCurrent();
			SecureTable table = kv.getValue();
			if(table == null) {
				log.warn("we could not modify table="+kv.getKey(), new RuntimeException().fillInStackTrace());
			}
			
			DboTableMeta tableMeta = table.getTableMeta();
			List<String> names = tableMeta.getColumnNameList();
			if(names.size() == 2 && timeSeriesNames.contains(names.get(0)) && timeSeriesNames.contains(names.get(1))) {
				long partitionSize = TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
				tableMeta.setTimeSeries(true);
				tableMeta.setTimeSeriesPartionSize(partitionSize);
				table.setTypeOfData(DataTypeEnum.TIME_SERIES);
				mgr2.put(tableMeta);
				mgr2.put(table);
			} else {
				table.setTypeOfData(DataTypeEnum.RELATIONAL);
				mgr2.put(table);
			}
			
			if(counter % 50 == 0) {
				mgr2.flush();
				mgr2.clear();
			}

			if(counter % 300 == 0) {
				log.info("ported "+counter+" records for cf=DboTableMeta time series changes and still porting");
			}
			counter++;
		}
		
		mgr2.clear();
		mgr2.flush();
		log.info("done porting. count="+counter+" records for cf=DboTableMeta time series changes");
	}

	private void buildIndexesOnNewSystem(NoSqlEntityManager mgr, NoSqlEntityManager mgr2) {
		String cf = "User";
		buildIndexForCf(mgr2, cf);
		cf = "AppProperty";
		buildIndexForCf(mgr2, cf);
		cf = "SdiTable";
		buildIndexForCf(mgr2, cf);
	}

	private void buildIndexForCf(NoSqlEntityManager mgr2, String cf) {
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
		log.info("done indexing. count="+counter+" records for cf="+cf);
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
				if(tb == null) {
					log.warn("Table='"+table.getName()+"' does not exist for some reason.  id looked up='"+table.getId()+"'");
					continue;
				}

				s.getTablesCursor().addElement(tb);
				tableCount++;
				counter++;
				if(counter % 50 == 0) {
					mgr2.put(s);
					mgr.clear();
					mgr2.flush();
					mgr2.clear();
				}
				
				if(counter % 500 == 0) {
					log.info("For db="+s.getName()+" ported over table count="+tableCount);
				}
			}

			mgr2.put(s);
			log.info("ported total tables="+tableCount+" for schema="+s.getName());
		}
		
		mgr.clear();
		mgr2.flush();
		log.info("counter is at="+counter+" for all tables CursorToMany port");
	}

}
