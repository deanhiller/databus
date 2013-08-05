package controllers.gui;

import gov.nrel.util.Utility;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.auth.GuiSecure;

import play.mvc.Controller;
import play.mvc.With;

import models.ChartDbo;
import models.DashboardSettings;
import models.EntityUser;
import models.UserSettings;

@With(GuiSecure.class)
public class Main extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void dashboard() {
		EntityUser user = Utility.getCurrentUser(session);
		UserSettings settings = user.getUserSettings();
		
		//"https://maps.google.com/?ie=UTF8&amp;ll=38.997934,-105.550567&amp;spn=4.362152,7.481689&amp;t=h&amp;z=7&amp;output=embed"
//		settings.setDashboardChart_1_uri("http://localhost:9000/charts/smallchart/FakeChart-js/eyJ0aXRsZSI6IkZha2UgQ2hhcnQiLCJ5YXhpc0xhYmVsIjoiTGVuZ3RoIiwidW5pdHMiOiJpbmNoZXMiLCJtc2ciOiJUaGlzIGlzIGEgZmFrZSBjaGFydCJ9");
//		settings.setDashboardChart_2_uri("http://localhost:9000/charts/smallchart/FakeChart-js/eyJ0aXRsZSI6IkZha2UgQ2hhcnQiLCJ5YXhpc0xhYmVsIjoiTGVuZ3RoIiwidW5pdHMiOiJpbmNoZXMiLCJtc2ciOiJUaGlzIGlzIGEgZmFrZSBjaGFydCJ9");
//		settings.setDashboardChart_3_uri("http://localhost:9000/charts/smallchart/FakeChart-js/eyJ0aXRsZSI6IkZha2UgQ2hhcnQiLCJ5YXhpc0xhYmVsIjoiTGVuZ3RoIiwidW5pdHMiOiJpbmNoZXMiLCJtc2ciOiJUaGlzIGlzIGEgZmFrZSBjaGFydCJ9");
//		settings.setDashboardChart_4_uri("https://maps.google.com/?ie=UTF8&amp;ll=38.997934,-105.550567&amp;spn=4.362152,7.481689&amp;t=h&amp;z=7&amp;output=embed");
		
		List<ChartDbo> charts = user.getCharts();
		render(user, charts);
	}
	
	public static void single(String id) {
		EntityUser user = Utility.getCurrentUser(session);
		ChartDbo selected = null;
		for(ChartDbo chartDbo : user.getCharts()) {
			if(chartDbo.getId().equals(id)) {
				selected = chartDbo;
				break;
			}
				
		}

		if(selected == null)
			notFound();

		String url = selected.getLargeChartUrl();
		List<ChartDbo> charts = user.getCharts();
		render(charts, url);
	}
}
