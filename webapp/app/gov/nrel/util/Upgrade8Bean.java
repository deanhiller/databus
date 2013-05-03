package gov.nrel.util;

import java.util.ArrayList;
import java.util.List;

import models.DataTypeEnum;
import models.SecureSchema;
import models.SecureTable;
import models.StreamAggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.Indexing;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;

public class Upgrade8Bean extends UpgradeAbstract {

	private static final Logger log = LoggerFactory.getLogger(Upgrade8Bean.class);
	
	public Upgrade8Bean() {

		super(8);
	}

	@Override
	protected void upgradeImpl() {
		if (log.isInfoEnabled())
			log.info("running upgrade 8 - setting *deviceMeta and *streamMeta to be searchable and relational");
		NoSqlEntityManager em = NoSql.em();

		if (log.isInfoEnabled())
			log.info("running upgrade 8");
		
		// add upgrade methods here
		fixRelationalTables();
		//em.flush();
		
	}

	private void fixRelationalTables() {
		makeTableSearchableAndRelational(SecureTable.findByName(NoSql.em(), "bacnetdeviceMeta"), null);
		makeTableSearchableAndRelational(SecureTable.findByName(NoSql.em(), "bacnetstreamMeta"), SecureTable.findByName(NoSql.em(), "bacnetdeviceMeta"));
		makeTableSearchableAndRelational(SecureTable.findByName(NoSql.em(), "modbusdeviceMeta"), null);
		makeTableSearchableAndRelational(SecureTable.findByName(NoSql.em(), "modbusstreamMeta"), SecureTable.findByName(NoSql.em(), "modbusdeviceMeta"));
	}
	
	private void makeTableSearchableAndRelational(SecureTable t, SecureTable parent) {
		if (t != null) {
			if (log.isInfoEnabled())
				log.info("Upgrading table "+t.getName());
			t.setSearchable(true);
			t.setTypeOfData(DataTypeEnum.RELATIONAL);
			if (parent != null) {
				if (log.isInfoEnabled())
					log.info("setting the parent of "+t.getName()+" to be "+parent.getName());
				t.setSearchableParentTable(parent.getName());
				t.setSearchableParentChildCol("device");
				t.setSearchableParentParentCol("id");
			}
			NoSql.em().put(t);
		}
		else
			if (log.isInfoEnabled())
				log.info("The table passed into upgrade8 is NULL!!! Assuming this is not production, not running upgrade!");
	}
	
	

}
