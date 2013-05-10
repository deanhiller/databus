package controllers.gui;

import gov.nrel.util.Utility;

import javax.persistence.Embedded;

import models.EntityUser;
import models.UserSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import play.mvc.Controller;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MySettings extends Controller {
	private static final Logger log = LoggerFactory.getLogger(MySettings.class);
	
	@Embedded
	private UserSettings settings;
	
	public static void mySettings() {
		EntityUser user = Utility.getCurrentUser(session);
		
		renderArgs.put("dashboard_enabled", user.getUserSettings().getDashboardEnabled());
		
		/**
		 * To Save:
		 * 		
		 * 		NoSql.em().put(user);
		 * 		NoSql.em().flush();
		 */
		
		render(user);
	}

}
