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

public class TransferBean2 extends TransferSuper {

	private static final Logger log = LoggerFactory.getLogger(TransferBean2.class);

	public void transfer() {
		String upgradeMode = (String) Play.configuration.get("upgrade.mode");
		if(upgradeMode == null || !"MONITOR".equals(upgradeMode)) {
			log.info("NOT running port monitor scripts since upgrade mode is not='MONITOR'");
			return; //we dont' run unless we are in transfer mode
		}

		NoSqlEntityManager mgr2 = initialize();
		NoSqlEntityManager mgr = NoSql.em();

		String cf = "MonitorDbo";
		portTableToNewCassandra(mgr, mgr2, cf);
		cf = "StreamAggregation";
		portTableToNewCassandra(mgr, mgr2, cf);
	}
}
