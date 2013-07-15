package gov.nrel.util;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.base.Bootstrap;
import com.alvazan.orm.api.base.DbTypeEnum;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.play.NoSql;

import play.Play;

public class TransferBean {

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
		
		//session.createQueryCursor("select u from Users, 500);
	}

}
