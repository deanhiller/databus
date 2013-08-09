package controllers.gui;

import gov.nrel.util.Utility;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Embedded;

import models.ChartDbo;
import models.DashboardSettings;
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
import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.With;
import play.vfs.VirtualFile;

import com.alvazan.play.NoSql;

import controllers.gui.auth.GuiSecure;
import controllers.gui.util.ChartUtil;

@With(GuiSecure.class)
public class Settings extends Controller {
	static final String EMBEDDED_CHART_ID = "databus-embedded";
	private static final Logger log = LoggerFactory.getLogger(Settings.class);
	
	public static void dashboardSettings() {
		EntityUser user = Utility.getCurrentUser(session);
		List<UserChart> userCharts = user.getUserCharts();

		List<ChartDbo> charts = user.getCharts();
		//Must add the chart with id =-1 which is the example chart...
		ChartDbo selected = new ChartDbo();
		selected.setEncodedVariables("eyJ0aXRsZSI6IkZha2UgQ2hhcnQiLCJ5YXhpc0xhYmVsIjoiTGVuZ3RoIiwidW5pdHMiOiJpbmNoZXMiLCJtc2ciOiJUaGlzIGlzIGEgZmFrZSBjaGFydCJ9");
		selected.setChartId("FakeChart-js");
		selected.setId("-1");
		charts.add(selected);
		
		render(user, userCharts, charts);
	}

	public static void charts() {
		EntityUser user = Utility.getCurrentUser(session);
		List<ChartDbo> charts = user.getCharts();
		
		render(charts);
	}

	public static void addChart() {
		EntityUser user = Utility.getCurrentUser(session);
		List<UserChart> userCharts = user.getUserCharts();
		render(userCharts);
	}

	public static void postSaveAccountSettings(EntityUser user) {
		EntityUser currentUser = Utility.getCurrentUser(session);
		if(!currentUser.getId().equals(user.getId())) {
			//security, only allow them to change their own dashboard
			badRequest("Sorry, this form did not come from that one");
		}

		//saving user saves the embedded stuff in the same row...
		NoSql.em().put(user);
		NoSql.em().flush();		

		dashboardSettings();
	}

	public static void postSaveChart(String title, String chartUrl) {
		EntityUser user = Utility.getCurrentUser(session);
		
		log.error("\nCHARTNAME: " + title);
		log.error("\nCHARTURL: " + chartUrl);

		ChartDbo chart = new ChartDbo();
		chart.setId(title);
		//fake the chart id for now to databus-embedded so we can intercept later
		chart.setChartId(EMBEDDED_CHART_ID);
		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put("url", chartUrl);
		variablesMap.put("title", title);
		String encoded = ChartUtil.encodeVariables(variablesMap);
		chart.setEncodedVariables(encoded);
		chart.setUrl(chartUrl);

		user.getCharts().add(chart);
		
		NoSql.em().put(user);
		NoSql.em().flush();
		
		charts();
	}

	public static void postDeleteChart(String id) {
		EntityUser user = Utility.getCurrentUser(session);
		
		for(ChartDbo chart : user.getCharts()) {
			if(id.equals(chart.getId())) {
				user.getCharts().remove(chart);
				break;
			}
		}
		
		NoSql.em().put(user);
		NoSql.em().flush();
		
		charts();
	}
}
