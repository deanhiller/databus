package controllers.gui;

import gov.nrel.util.Utility;
import models.EntityUser;
import controllers.gui.auth.GuiSecure;
import play.mvc.Controller;
import play.mvc.With;

import play.Play;

@With(GuiSecure.class)
public class Search extends Controller{

	//NOTE: These are definitely not in the right place...
	public static void myHelpBootstrapProto_Summary() {
		EntityUser user = Utility.getCurrentUser(session);
		render(user);
	}
	
	public static void dashboardSide() {
		String chartsEnabled = Play.configuration.getProperty("gui.charting.enabled");
		String chartsLibrary = Play.configuration.getProperty("gui.charting.library");
		
		renderArgs.put("charts_enabled", chartsEnabled);
		renderArgs.put("charts_library", chartsLibrary);
		
		EntityUser user = Utility.getCurrentUser(session);
		render(user);
	}
	
}
