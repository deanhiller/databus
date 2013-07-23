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

public class TransferBean extends TransferSuper {

	private static final Logger log = LoggerFactory.getLogger(TransferBean.class);

	protected void transferImpl(NoSqlEntityManager mgr2) {
		String upgradeMode = (String) Play.configuration.get("upgrade.mode");
		if(upgradeMode == null || !"MONITOR".equals(upgradeMode))
			return; //we dont' run unless we are in transfer mode

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
		cf = "DboTableMeta";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "DboColumnMeta";
		portTableToNewCassandra(mgr, mgr2, cf);
				
		//time to port indexes now...
		buildIndexesOnNewSystem(mgr, mgr2);

		//Now that we have indexes, we can port cursor to many
		//need indexes first since we use indexes on new cassandra(and use one on old cassandra as well).
		portOverCursorToMany(mgr, mgr2);
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

}
