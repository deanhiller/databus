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
		
		// See if the dashboard is enabled
		renderArgs.put("dashboard_enabled", user.getUserSettings().getDashboardEnabled());
		
		// Get the current options
		renderArgs.put("dashboard_chart_count", user.getUserSettings().getDashboardChartCount());
		
		renderArgs.put("user_chart_1", user.getUserSettings().getDashboardChart_1());
		renderArgs.put("user_chart_2", user.getUserSettings().getDashboardChart_2());
		renderArgs.put("user_chart_3", user.getUserSettings().getDashboardChart_3());
		renderArgs.put("user_chart_4", user.getUserSettings().getDashboardChart_4());
		
		render(user);
	}
	
}
