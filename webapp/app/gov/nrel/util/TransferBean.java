package gov.nrel.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.DbTypeEnum;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
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
		
		Map<String, Object> props = new HashMap<String, Object>();
		NoSqlEntityManagerFactory factory = Bootstrap.create(DbTypeEnum.CASSANDRA, props );
		NoSqlEntityManager mgr2 = factory.createEntityManager();
		NoSqlEntityManager mgr = NoSql.em();
		
		NoSqlTypedSession session = mgr.getTypedSession();
		NoSqlTypedSession session2 = mgr2.getTypedSession();

		String cf = "DboColumnMeta";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "DboTableMeta";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "AppProperty";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "EntityGroupXref";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "KeyToTableName";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "SdiColumn";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "SdiTable";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "SecureResourceGroupXref";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "SecurityGroup";
		portTableToNewCassandra(mgr, session, session2, cf);
		cf = "WebNodeDbo";
		portTableToNewCassandra(mgr, session, session2, cf);
	}

	private void portTableToNewCassandra(NoSqlEntityManager mgr,
			NoSqlTypedSession session, NoSqlTypedSession session2, String cf) {
		log.info("starting port of Column Family="+cf);
		QueryResult result = session.createQueryCursor("select u from "+cf+" as u", 500);
		Cursor<KeyValue<TypedRow>> cursor = result.getPrimaryViewCursor();

		int counter = 0;
		while(cursor.next()) {
			TypedRow row = cursor.getCurrent().getValue();
			TypedRow newRow = session2.createTypedRow(cf);
			newRow.setRowKey(row.getRowKey());
			Collection<TypedColumn> coll = row.getColumnsAsColl();
			Iterator<TypedColumn> iterator = coll.iterator();
			while(iterator.hasNext()) {
				TypedColumn col = iterator.next();
				byte[] name = col.getNameRaw();
				byte[] value = col.getValueRaw();
				newRow.addColumn(name, value, null);
			}

			session2.put(cf, newRow);
			counter++;
			if(counter % 50 == 0) {
				mgr.clear();
				session2.flush();
			}
			
			if(counter % 300 == 0) {
				log.info("done porting "+counter+" records for cf="+cf);
			}
		}

		//If the last batch has less than 50, we need to flush that too....
		mgr.clear();
		session2.flush();
	}

}
