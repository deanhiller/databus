package models;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;

@NoSqlEmbeddable
public class UserSettings {
	private String dashboardEnabled;
	private String dashboardChartCount;
	
	public UserSettings() {
		this.dashboardEnabled = "true";
		this.dashboardChartCount = "1";
	}
	
	public UserSettings(String dashboard, String chartCount) {
		this.dashboardEnabled = dashboard;
		this.dashboardChartCount = chartCount;
	}

	public String getDashboardEnabled() {
		return dashboardEnabled;
	}

	public void setDashboardEnabled(String dashboardEnabled) {
		this.dashboardEnabled = dashboardEnabled;
	}

	public String getDashboardChartCount() {
		return dashboardChartCount;
	}

	public void setDashboardChartCount(String dashboardChartCount) {
		this.dashboardChartCount = dashboardChartCount;
	}
}
