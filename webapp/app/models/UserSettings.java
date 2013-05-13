package models;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;

@NoSqlEmbeddable
public class UserSettings {
	private String dashboardEnabled;
	private String dashboardChartCount;
	private String dashboardChart_1;
	private String dashboardChart_2;
	private String dashboardChart_3;
	private String dashboardChart_4;
	
	public UserSettings() {
		this.dashboardEnabled = "true";
		this.dashboardChartCount = "4";
		dashboardChart_1 = "BLANK";
		dashboardChart_2 = "BLANK";
		dashboardChart_3 = "BLANK";
		dashboardChart_4 = "BLANK";
	}
	
	public UserSettings(String dashboard, String chartCount) {
		this.dashboardEnabled = dashboard;
		this.dashboardChartCount = chartCount;
		dashboardChart_1 = "BLANK";
		dashboardChart_2 = "BLANK";
		dashboardChart_3 = "BLANK";
		dashboardChart_4 = "BLANK";
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

	public String getDashboardChart_1() {
		return dashboardChart_1;
	}

	public void setDashboardChart_1(String dashboardChart_1) {
		this.dashboardChart_1 = dashboardChart_1;
	}

	public String getDashboardChart_2() {
		return dashboardChart_2;
	}

	public void setDashboardChart_2(String dashboardChart_2) {
		this.dashboardChart_2 = dashboardChart_2;
	}

	public String getDashboardChart_3() {
		return dashboardChart_3;
	}

	public void setDashboardChart_3(String dashboardChart_3) {
		this.dashboardChart_3 = dashboardChart_3;
	}

	public String getDashboardChart_4() {
		return dashboardChart_4;
	}

	public void setDashboardChart_4(String dashboardChart_4) {
		this.dashboardChart_4 = dashboardChart_4;
	}
}
