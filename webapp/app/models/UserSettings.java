package models;

import java.util.List;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;

@NoSqlEmbeddable
public class UserSettings {
	private String dashboardEnabled;
	private String dashboardChartCount;
	private String dashboardChart_1;
	private String dashboardChart_1_type;
	private String dashboardChart_1_uri;
	private String dashboardChart_2;
	private String dashboardChart_2_type;
	private String dashboardChart_2_uri;
	private String dashboardChart_3;
	private String dashboardChart_3_type;
	private String dashboardChart_3_uri;
	private String dashboardChart_4;
	private String dashboardChart_4_type;
	private String dashboardChart_4_uri;
	
	public UserSettings() {
		this.dashboardEnabled = "true";
		this.dashboardChartCount = "4";
		dashboardChart_1 = "BLANK";
		dashboardChart_1_type = "BLANK";
		dashboardChart_1_uri = "BLANK";
		dashboardChart_2 = "BLANK";
		dashboardChart_2_type = "BLANK";
		dashboardChart_2_uri = "BLANK";
		dashboardChart_3 = "BLANK";
		dashboardChart_3_type = "BLANK";
		dashboardChart_4_uri = "BLANK";
		dashboardChart_4 = "BLANK";
		dashboardChart_4_type = "BLANK";
		dashboardChart_4_uri = "BLANK";
	}
	
	public UserSettings(String dashboard, String chartCount) {
		this.dashboardEnabled = dashboard;
		this.dashboardChartCount = chartCount;
		dashboardChart_1 = "BLANK";
		dashboardChart_1_type = "BLANK";
		dashboardChart_1_uri = "BLANK";
		dashboardChart_2 = "BLANK";
		dashboardChart_2_type = "BLANK";
		dashboardChart_2_uri = "BLANK";
		dashboardChart_3 = "BLANK";
		dashboardChart_3_type = "BLANK";
		dashboardChart_4_uri = "BLANK";
		dashboardChart_4 = "BLANK";
		dashboardChart_4_type = "BLANK";
		dashboardChart_4_uri = "BLANK";
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

	public String getDashboardChart_1_type() {
		return dashboardChart_1_type;
	}

	public void setDashboardChart_1_type(String dashboardChart_1_type) {
		this.dashboardChart_1_type = dashboardChart_1_type;
	}

	public String getDashboardChart_2_type() {
		return dashboardChart_2_type;
	}

	public void setDashboardChart_2_type(String dashboardChart_2_type) {
		this.dashboardChart_2_type = dashboardChart_2_type;
	}

	public String getDashboardChart_3_type() {
		return dashboardChart_3_type;
	}

	public void setDashboardChart_3_type(String dashboardChart_3_type) {
		this.dashboardChart_3_type = dashboardChart_3_type;
	}

	public String getDashboardChart_4_type() {
		return dashboardChart_4_type;
	}

	public void setDashboardChart_4_type(String dashboardChart_4_type) {
		this.dashboardChart_4_type = dashboardChart_4_type;
	}

	public String getDashboardChart_1_uri() {
		return dashboardChart_1_uri;
	}

	public void setDashboardChart_1_uri(String dashboardChart_1_uri) {
		this.dashboardChart_1_uri = dashboardChart_1_uri;
	}

	public String getDashboardChart_2_uri() {
		return dashboardChart_2_uri;
	}

	public void setDashboardChart_2_uri(String dashboardChart_2_uri) {
		this.dashboardChart_2_uri = dashboardChart_2_uri;
	}

	public String getDashboardChart_3_uri() {
		return dashboardChart_3_uri;
	}

	public void setDashboardChart_3_uri(String dashboardChart_3_uri) {
		this.dashboardChart_3_uri = dashboardChart_3_uri;
	}

	public String getDashboardChart_4_uri() {
		return dashboardChart_4_uri;
	}

	public void setDashboardChart_4_uri(String dashboardChart_4_uri) {
		this.dashboardChart_4_uri = dashboardChart_4_uri;
	}
}
