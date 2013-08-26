package controllers.gui;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import controllers.gui.auth.GuiSecure;

import play.mvc.Controller;
import play.mvc.With;
import gov.nrel.util.Utility;
import models.Entity;
import models.EntityGroup;
import models.EntityUser;
import models.SecureResourceGroupXref;
import models.SecureSchema;

@With(GuiSecure.class)
public class MySummary extends Controller {
	private static final Logger log = LoggerFactory.getLogger(MySummary.class);

	public static void mySummary() {
		EntityUser user = Utility.getCurrentUser(session);
		
		List<EntityGroup> groups = EntityGroup.findAll(NoSql.em());
		List<SecureSchema> databases = SecureSchema.findAll(NoSql.em());

		for (SecureSchema s : databases) {
			List<SecureResourceGroupXref> refs = s.getEntitiesWithAccess();
			for (SecureResourceGroupXref ref : refs) {
				Entity entity = ref.getUserOrGroup();
				if (log.isInfoEnabled())
					log.info("entity=" + entity+" perm="+ref.getPermission());
			}
		}
		String sid = session.get("sid");

		if (log.isInfoEnabled())
			log.info("render index. user=" + user + " num tables=" + databases.size()
				+ " groupscnt=" + groups.size() + " session=" + sid);
		render(groups, databases);
	}

}
