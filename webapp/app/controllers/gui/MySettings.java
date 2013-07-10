package controllers.gui;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Embedded;

import models.EntityUser;
import models.ScriptChart;
import models.UserChart;
import models.UserChart.ChartLibrary;
import models.UserChart.ChartType;
import models.UserSettings;
import models.comparitors.UserChartComparitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;

import com.alvazan.play.NoSql;

import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MySettings extends Controller {
	private static final Logger log = LoggerFactory.getLogger(MySettings.class);
	
	@Embedded
	private UserSettings settings;
	
	private static String currentRedirectTo = "";
	
	public static void mySettings() {
		String redirectTo = "";
		
		if((MySettings.currentRedirectTo != null) && (!MySettings.currentRedirectTo.equals(""))) {
			redirectTo = MySettings.currentRedirectTo;
		}
		
		MySettings.currentRedirectTo = "";
		
		log.error("mySettings REDIRECTTO IS: " + redirectTo);
		
		EntityUser user = Utility.getCurrentUser(session);
		List<UserChart> userCharts = user.getUserCharts();
		
		render(userCharts, redirectTo);
	}
	
	public static void myDashboardSettings() {
		EntityUser user = Utility.getCurrentUser(session);
		
		// Set current options
		renderArgs.put("dashboard_enabled", user.getUserSettings().getDashboardEnabled());
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
		
		/**
		 * We need to get all of the charts the user has currently saved
		 */
		List<UserChart> userCharts = user.getUserCharts();	
		
		render(user, userCharts);
	}
	
	public static void myChartsSettings() {
		EntityUser user = Utility.getCurrentUser(session);
		
		/**
		 * We need to get all of the charts the user has currently saved
		 */
		List<UserChart> userCharts = user.getUserCharts();	
		Collections.sort(userCharts, new UserChartComparitor());
		
		render(userCharts);
	}
	
	public static void addChartSettings() {
		EntityUser user = Utility.getCurrentUser(session);
		
		/**
		 * Lets get a list of all charts in the charts directory for 
		 */
		List<ScriptChart> scriptCharts = new ArrayList<ScriptChart>();
		VirtualFile highchartsDir = Play.getVirtualFile("/public/Charts/HighCharts/");
		List<VirtualFile> highCharts = highchartsDir.list();
		for(VirtualFile vfile : highCharts) {
			String chartData = vfile.contentAsString();
			if(chartData.contains("|DATABUS_CHART|")) {
				/**
				 * This is a legit chart (enough to know w/out fully validating it)
				 */
				ScriptChart scriptChart = new ScriptChart(chartData, vfile.getName());
				if(scriptChart.validate()) {
					scriptCharts.add(scriptChart);
				}
			}
		}
		
		render(scriptCharts);
	}
	
	public static void deleteChart(String chartToDelete) {		
		log.error("\n\nDELETE CHART CALLED: [" + chartToDelete + "]");
		
		if((chartToDelete != null) && (!chartToDelete.equals(""))) {
			EntityUser user = Utility.getCurrentUser(session);
			List<UserChart> userCharts = user.getUserCharts();
			
			int chartIndex = 0;
			boolean found = false;
			for(UserChart userChart : userCharts) {
				if(userChart.getChartName().equals(chartToDelete)) {
					found = true;
					break;
				}
				
				chartIndex++;
			}
			
			if(found) {
				userCharts.remove(chartIndex);
				
				user.setUserCharts(userCharts);
				NoSql.em().put(user);
				NoSql.em().flush();
			}
		}		
		
		MySettings.currentRedirectTo = "mycharts_settings";
		
		mySettings();
	}
		
	public static void postSaveEmbeddedChartSettings(String embedded_chart_name, String embedded_chart_url) {
		EntityUser user = Utility.getCurrentUser(session);
		
		log.error("\nCHARTNAME: " + embedded_chart_name);
		log.error("\nCHARTURL: " + embedded_chart_url);
		
		UserChart newUserChart = new UserChart(ChartType.EMBEDDED_OBJECT, ChartLibrary.HIGHCHARTS, embedded_chart_name, embedded_chart_url);
		user.addChart(newUserChart);
		
		NoSql.em().put(user);
		NoSql.em().flush();
		
		MySettings.currentRedirectTo = "mycharts_settings";
		
		log.error("POST FIELD: REDIRECTTO IS: " + MySettings.currentRedirectTo);
		
		mySettings();
	}
	
	public static void postSaveScriptChartSettings(String script_chart_filename, String script_chart_name, String script_chart_title, String script_chart_db) {
		EntityUser user = Utility.getCurrentUser(session);
		
		log.error("\nFILENAME: " + script_chart_filename);
		log.error("\nNAME: " + script_chart_name);
		log.error("\nTITLE: " + script_chart_title);
		log.error("\nDB: " + script_chart_db);
		
		UserChart newUserChart = new UserChart(ChartType.SCRIPT, ChartLibrary.HIGHCHARTS, script_chart_name, script_chart_filename);
		
		if((script_chart_title != null) && (!script_chart_title.equals(""))) {
			newUserChart.setScriptChart_ChartTitleOverride(script_chart_title);
		}
		
		if((script_chart_db != null) && (!script_chart_db.equals(""))) {
			newUserChart.setScriptChart_SingleDBOverride(script_chart_db);
		}
		
		user.addChart(newUserChart);
		
		NoSql.em().put(user);
		NoSql.em().flush();
		
		MySettings.currentRedirectTo = "mycharts_settings";
		
		log.error("POST FIELD: REDIRECTTO IS: " + MySettings.currentRedirectTo);
		
		mySettings();
	}
	
	public static void postSaveAccountSettings(String dashboard_enabled, String dashboard_chart_count, String dashboard_chart1_select,
																	 String dashboard_chart2_select, String dashboard_chart3_select, String dashboard_chart4_select,
																	 String dashboard_chart5_select, String dashboard_chart6_select, String dashboard_chart7_select,
																	 String dashboard_chart8_select, String dashboard_chart9_select, String dashboard_chart10_select) {
		EntityUser user = Utility.getCurrentUser(session);
		UserSettings userSettings = user.getUserSettings();
				
		String enabled = "true";
		if(dashboard_enabled == null) {
			enabled = "false";
		}
		userSettings.setDashboardEnabled(enabled);
		userSettings.setDashboardChartCount(dashboard_chart_count);
		
		String dashboardChart_1 = dashboard_chart1_select;
		String dashboardChart_2 = "BLANK";
		String dashboardChart_3 = "BLANK";
		String dashboardChart_4 = "BLANK";
		
		if(dashboard_chart_count.endsWith("2")) {
			dashboardChart_1 = dashboard_chart2_select;
			dashboardChart_2 = dashboard_chart3_select;
		} else if(dashboard_chart_count.endsWith("3")) {
			dashboardChart_1 = dashboard_chart4_select;
			dashboardChart_2 = dashboard_chart5_select;
			dashboardChart_3 = dashboard_chart6_select;
		} else if(dashboard_chart_count.endsWith("4")) {
			dashboardChart_1 = dashboard_chart7_select;
			dashboardChart_2 = dashboard_chart8_select;
			dashboardChart_3 = dashboard_chart9_select;
			dashboardChart_4 = dashboard_chart10_select;
		}
		
		/**
		 * Now lets get each chart's data from their chart list
		 */
		boolean found_1 = false;
		boolean found_2 = false;
		boolean found_3 = false;
		boolean found_4 = false;
		List<UserChart> userCharts = user.getUserCharts();
		for(UserChart userChart : userCharts) {
			/**
			 * Do dashboard chart 1
			 */
			if(!found_1) {
				if(dashboardChart_1.equals("BLANK")) {
					found_1 = true;
					userSettings.setDashboardChart_1("BLANK");
					userSettings.setDashboardChart_1_type("BLANK");
					userSettings.setDashboardChart_1_uri("BLANK");
				} else {
					if(userChart.getChartName().equals(dashboardChart_1)) {
						found_1 = true;
						userSettings.setDashboardChart_1(dashboardChart_1);
						userSettings.setDashboardChart_1_type(userChart.getChartType().getName());			
						userSettings.setDashboardChart_1_uri(userChart.getTrueChartURI());
					}
				}
			}
			
			/**
			 * Do dashboard chart 2
			 */
			if(!found_2) {
				if(dashboardChart_2.equals("BLANK")) {
					found_2 = true;
					userSettings.setDashboardChart_2("BLANK");
					userSettings.setDashboardChart_2_type("BLANK");
					userSettings.setDashboardChart_2_uri("BLANK");
				} else {
					if(userChart.getChartName().equals(dashboardChart_2)) {
						found_2 = true;
						userSettings.setDashboardChart_2(dashboardChart_2);
						userSettings.setDashboardChart_2_type(userChart.getChartType().getName());							
						userSettings.setDashboardChart_2_uri(userChart.getTrueChartURI());
					}
				}
			}
			
			/**
			 * Do dashboard chart 3
			 */
			if(!found_3) {
				if(dashboardChart_3.equals("BLANK")) {
					found_3 = true;
					userSettings.setDashboardChart_3("BLANK");
					userSettings.setDashboardChart_3_type("BLANK");
					userSettings.setDashboardChart_3_uri("BLANK");
				} else {
					if(userChart.getChartName().equals(dashboardChart_3)) {
						found_3 = true;
						userSettings.setDashboardChart_3(dashboardChart_3);
						userSettings.setDashboardChart_3_type(userChart.getChartType().getName());							
						userSettings.setDashboardChart_3_uri(userChart.getTrueChartURI());
					}
				}
			}
			
			/**
			 * Do dashboard chart 4
			 */
			if(!found_4) {
				if(dashboardChart_4.equals("BLANK")) {
					found_4 = true;
					userSettings.setDashboardChart_4("BLANK");
					userSettings.setDashboardChart_4_type("BLANK");
					userSettings.setDashboardChart_4_uri("BLANK");
				} else {
					if(userChart.getChartName().equals(dashboardChart_4)) {
						found_4 = true;
						userSettings.setDashboardChart_4(dashboardChart_4);
						userSettings.setDashboardChart_4_type(userChart.getChartType().getName());							
						userSettings.setDashboardChart_4_uri(userChart.getTrueChartURI());
					}
				}
			}
			
			if(found_1 && found_2 && found_3 && found_4) {
				break;
			}
		}
		
		/**
		 * To Save:
		 * 		
		 * 		NoSql.em().put(user);
		 * 		NoSql.em().flush();
		 */
		NoSql.em().put(user);
		NoSql.em().flush();		
		
		mySettings();
	}
}
