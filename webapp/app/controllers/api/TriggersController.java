package controllers.api;

import gov.nrel.util.ATriggerListener;
import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import models.EntityUser;
import models.PermissionType;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.StreamAggregation;
import models.message.RegisterMessage;
import models.message.Trigger;
import models.message.Triggers;

import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import controllers.Parsing;
import controllers.SecurityUtil;
import controllers.TableMonitor;
import play.mvc.Controller;

public class TriggersController extends Controller {

	private static final Logger log = LoggerFactory.getLogger(TriggersController.class);

	public static void postAdd() {
		String json = Parsing.fetchJson();
		Trigger msg = Parsing.parseJson(json, Trigger.class);

		long tenMinutes = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
		if(msg.getId() == null || "".equals(msg.getId())) 
			badRequest("id cannot be null");
		else if(msg.getUrl() == null || "".equals(msg.getUrl()))
			badRequest("url cannot be null nor empty");
		else if(msg.getRate() < tenMinutes)
			badRequest("rate cannot be less than "+tenMinutes+"(10 minutes)");
		else if(msg.getDatabase() == null)
			badRequest("The database attribute must be specified so we know the owning database");
		
		String database = msg.getDatabase();
		String url = msg.getUrl();
		validateIfLogV1InUrl(url);

		String id = formId(msg.getId());

		EntityUser user = Utility.getCurrentUserNew(session);

		CronService svc = CronServiceFactory.getSingleton(null);
		PlayOrmCronJob job = svc.getMonitor(id);
		if(job != null)
			badRequest("id="+id+" is already in use.  please delete and recreate if that is what you want");
		SecureSchema db = SecureSchema.findByName(NoSql.em(), database);
		job = ATriggerListener.transform(msg, id, db, user);
		
		//run trigger here as test...
		ATriggerListener listener = new ATriggerListener(svc, 0, 1);
		try {
			listener.monitorFired(job);
		} catch(Exception e) {
			log.warn("Exception", e);
			badRequest("Your url is most likely invalid.  exception trying to run your trigger from start=0 to end=1="+e.getMessage());
		}

		svc.saveMonitor(job);

		//add the job to the schema as well....
		List<String> ids = db.getTriggerIds();
		ids.add(job.getId());

		NoSql.em().put(db);
		NoSql.em().flush();

		ok();
	}

	private static void validateIfLogV1InUrl(String url) {
		String logTable = findTableName(url);
		if(logTable == null)
			return;
		SecureTable table = SecureTable.findByName(NoSql.em(), logTable);
		if(table == null) {
			badRequest("This table="+logTable+" does not exist");
		} else if(!table.isForLogging()) 
			badRequest("The table="+logTable+" is not registered as a logging table");
		String name = table.getName();
		permissionCheck(name);
	}

	private static String findTableName(String url) {
		int index = url.indexOf("logV1/");
		if(index < 0)
			return null;

		String theRest = url.substring(index+"logV1".length()+1);
		int index2 = theRest.indexOf("/");
		if(index2 < 0)
			badRequest("There is no / found after the logV1/.  For example, you should have logV1/{logTable}/module/");
		String logTable = theRest.substring(0, index2);
		return logTable;
	}

	private static void permissionCheck(String name) {
		PermissionType permission = SecurityUtil.checkSingleTable2(name);
		if(PermissionType.ADMIN.isHigherRoleThan(permission))
			unauthorized("You are not authorized as an admin of database for table="+name);
	}

	private static String formId(String origId) {
		String id = "_log"+origId;
		return id;
	}
	
	public static void postDelete(String triggerId) {
		String id = formId(triggerId);
		CronService svc = CronServiceFactory.getSingleton(null);
		PlayOrmCronJob job = svc.getMonitor(id);

		String tableName = findTableName(job.getProperties().get("url"));
		permissionCheck(tableName);
		svc.deleteMonitor(id);

		SecureTable table = SecureTable.findByName(NoSql.em(), tableName);
		SecureSchema schemaDbo = table.getSchema();
		schemaDbo.getTriggerIds().remove(id);
		NoSql.em().put(schemaDbo);
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
		else if(!ref.isAdminRole()) 
			unauthorized("You are not an admin of database="+database);

		List<String> triggerIds = db.getTriggerIds();

		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> jobs = svc.getMonitors(triggerIds);
		Triggers triggers = new Triggers();

		for(PlayOrmCronJob job : jobs) {
			Trigger trigger = ATriggerListener.transform(job);
			triggers.getTriggers().add(trigger);
		}

		renderJSON(triggers);
	}

	public static long toLong(String val) {
		return Long.parseLong(val);
	}

}
