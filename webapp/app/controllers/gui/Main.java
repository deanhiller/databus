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

	public static void dashboard() {
		EntityUser user = Utility.getCurrentUser(session);
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
