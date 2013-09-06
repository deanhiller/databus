package controllers.api;

import gov.nrel.util.ATriggerListener;
import gov.nrel.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.EntityUser;
import models.PermissionType;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.message.PostTrigger;
import models.message.PostTriggers;
import models.message.Trigger;
import models.message.Triggers;

import org.apache.commons.lang.StringUtils;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;

import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

import controllers.Parsing;
import controllers.SecurityUtil;

public class TriggerController extends Controller {

	private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

	public static void postAdd() {
		String json = Parsing.fetchJson();
		PostTrigger msg = Parsing.parseJson(json, PostTrigger.class);

		if(StringUtils.isEmpty(msg.getDatabase()))
			badRequest("database cannot be null");
		else if(StringUtils.isEmpty(msg.getTable()))
			badRequest("table cannot be null");
		else if(StringUtils.isEmpty(msg.getScriptLanguage()))
			badRequest("scriptLanguage cannot be null");
		else if(StringUtils.isEmpty(msg.getScript()))
			badRequest("script cannot be null");

		SecureTable sdiTable = SecurityUtil.checkSingleTable(msg.getTable());
		DboTableMeta tableMeta = sdiTable.getTableMeta();
		SecureSchema schema = sdiTable.getSchema();
		String name = schema.getName();
		if(!name.equals(msg.getDatabase()))
			badRequest("table="+msg.getTable()+" is not in database="+msg.getDatabase());

		PostTrigger.transform(tableMeta, msg);
		
		List<String> ids = schema.getPostTriggerIds();
		ids.add(tableMeta.getColumnFamily());
		
		NoSql.em().put(tableMeta);
		NoSql.em().put(schema);
		NoSql.em().flush();

		ok();
	}

//	private static void permissionCheck(String name) {
//		PermissionType permission = SecurityUtil.checkSingleTable2(name);
//		if(PermissionType.ADMIN.isHigherRoleThan(permission))
//			unauthorized("You are not authorized as an admin of database for table="+name);
//	}
	
	public static void postDelete(String tableName) {
		SecureTable table = SecurityUtil.checkSingleTable(tableName);
		SecureSchema db = table.getSchema();
		
		DboTableMeta tableMeta = table.getTableMeta();
		Map<String, String> extensions = tableMeta.getExtensions();
		extensions.remove("databus.lang");
		extensions.remove("databus.script");
		extensions.remove("databus.callback");

		db.getPostTriggerIds().remove(tableName);

		NoSql.em().put(tableMeta);
		NoSql.em().put(db);
		NoSql.em().flush();
	}

	public static void list(String database) {
		SecureSchema db = SecureSchema.findByName(NoSql.em(), database);
		if(db == null)
			notFound("There is no database called="+database);
		Map<String, SecureResourceGroupXref> dbIdToRef = SecurityUtil.groupSecurityCheck();
		SecureResourceGroupXref ref = dbIdToRef.get(db.getId());
		if(ref == null)
			unauthorized("You do not have access to database="+database);

		List<String> postTriggerIds = db.getPostTriggerIds();
		Cursor<KeyValue<DboTableMeta>> tables = NoSql.em().findAll(DboTableMeta.class, postTriggerIds);

		PostTriggers triggers = new PostTriggers();

		while(tables.next()) {
			KeyValue<DboTableMeta> current = tables.getCurrent();
			DboTableMeta table = current.getValue();
			PostTrigger t = PostTrigger.transform(table, database);
			triggers.getTriggers().add(t);
		}

		renderJSON(triggers);
	}

}
