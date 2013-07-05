package controllers.gui;

import gov.nrel.util.Utility;

import java.util.List;

import models.EntityUser;
import models.UserChart;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;

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
		renderArgs.put("user_chart_1_type", user.getUserSettings().getDashboardChart_1_type());
		renderArgs.put("user_chart_1_uri", user.getUserSettings().getDashboardChart_1_uri());
		renderArgs.put("user_chart_2", user.getUserSettings().getDashboardChart_2());
		renderArgs.put("user_chart_2_type", user.getUserSettings().getDashboardChart_2_type());
		renderArgs.put("user_chart_2_uri", user.getUserSettings().getDashboardChart_2_uri());
		renderArgs.put("user_chart_3", user.getUserSettings().getDashboardChart_3());
		renderArgs.put("user_chart_3_type", user.getUserSettings().getDashboardChart_3_type());
		renderArgs.put("user_chart_3_uri", user.getUserSettings().getDashboardChart_3_uri());
		renderArgs.put("user_chart_4", user.getUserSettings().getDashboardChart_4());
		renderArgs.put("user_chart_4_type", user.getUserSettings().getDashboardChart_4_type());
		renderArgs.put("user_chart_4_uri", user.getUserSettings().getDashboardChart_4_uri());
		
		List<UserChart> userCharts = user.getUserCharts();	
		
		render(user, userCharts);
	}
	
}
