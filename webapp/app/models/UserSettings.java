package models;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;

@NoSqlEmbeddable
public class UserSettings {
	private boolean dashboardEnabled;
	
	public UserSettings() {
		this.dashboardEnabled = true;
	}
	
	public UserSettings(boolean dashboard) {
		this.dashboardEnabled = dashboard;
	}

	public boolean isDashboardEnabled() {
		return dashboardEnabled;
	}

	public void setDashboardEnabled(boolean dashboardEnabled) {
		this.dashboardEnabled = dashboardEnabled;
	}
}
