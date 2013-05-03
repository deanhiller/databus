package controllers;

import gov.nrel.util.Utility;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import models.Entity;
import models.EntityGroup;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Invoker;
import play.mvc.Controller;
import play.mvc.Scope.Session;

import com.alvazan.play.NoSql;

public class Application extends Controller {

	private static final Logger log = LoggerFactory
			.getLogger(Application.class);

	public static void index() {
		EntityUser user = Utility.getCurrentUser(session);

		List<EntityGroup> groups = EntityGroup.findAll(NoSql.em());
		List<SecureSchema> schemas = SecureSchema.findAll(NoSql.em());

		for (SecureSchema s : schemas) {
			List<SecureResourceGroupXref> refs = s.getEntitiesWithAccess();
			for (SecureResourceGroupXref ref : refs) {
				Entity entity = ref.getUserOrGroup();
				if (log.isInfoEnabled())
					log.info("entity=" + entity);
			}
		}
		String sid = session.get("sid");

		if (log.isInfoEnabled())
			log.info("render index. user=" + user + " num tables=" + schemas.size()
				+ " groupscnt=" + groups.size() + " session=" + sid);
		render(groups, schemas);
	}

	public static void viewTable(String tableName) {
		SecureResource targetTable = SecureTable.findByName(NoSql.em(),
				tableName);
		if (targetTable == null)
			notFound();

		render(targetTable);
	}

	public static void pingTest() {
		EntityUser user = Utility.getCurrentUser(session);
		
		List<EntityGroup> groups = EntityGroup.findAll(NoSql.em());
		List<SecureSchema> schemas = SecureSchema.findAll(NoSql.em());

		for (SecureSchema s : schemas) {
			List<SecureResourceGroupXref> refs = s.getEntitiesWithAccess();
			for (SecureResourceGroupXref ref : refs) {
				Entity entity = ref.getUserOrGroup();
				log.info("entity=" + entity);
			}
		}
		String sid = session.get("sid");

		log.info("ping test!!!! index. user=" + user + " num tables=" + schemas.size()
				+ " groupscnt=" + groups.size() + " session=" + sid);
		
		render();
	}

	public static void pingTest2() {
		render();
	}
	
	public static void numRequests() {
		renderText("not done yet");
	}
}