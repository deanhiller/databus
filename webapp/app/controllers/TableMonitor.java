package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.playorm.cron.api.PlayOrmCronJob;

public class TableMonitor {

	private String id;
	
	private String schema;
	private String tableName;
	private String emails;
	private long updatePeriod;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getEmails() {
		return emails;
	}
	public void setEmails(String emails) {
		this.emails = emails;
	}
	public long getUpdatePeriod() {
		return updatePeriod;
	}
	public void setUpdatePeriod(long timePeriod) {
		this.updatePeriod = timePeriod;
	}
	public static String formKey(String schema, String tableName2) {
		return schema+"-"+tableName2;
	}

	public static PlayOrmCronJob copy(String schema, TableMonitor monitor) {
		PlayOrmCronJob m = new PlayOrmCronJob();
		String id = TableMonitor.formKey(schema, monitor.getTableName());
		m.setId(id);
		long millis = TimeUnit.MILLISECONDS.convert(monitor.getUpdatePeriod(), TimeUnit.MINUTES);
		m.setTimePeriodMillis(millis);
		m.getProperties().put("emails", monitor.getEmails());
		m.getProperties().put("table", monitor.getTableName());
		m.getProperties().put("schema", schema);
		return m;
	}
	
	public static TableMonitor copy(PlayOrmCronJob m) {
		TableMonitor monitor = new TableMonitor();
		monitor.setTableName(m.getProperties().get("table"));
		monitor.setSchema(m.getProperties().get("schema"));
		monitor.setId(m.getId());
		long minutes = TimeUnit.MINUTES.convert(m.getTimePeriodMillis(), TimeUnit.MILLISECONDS);
		monitor.setUpdatePeriod(minutes);
		monitor.setEmails(m.getProperties().get("emails"));
		return monitor;
	}
	
	public List<String> getEmailsAsList() {
		if (StringUtils.isNotBlank(emails)) {
			String[] split = emails.split(",");
			List<String> emails = new ArrayList<String>();
			for(String s : split) {
				emails.add(s.trim());
			}
			return emails;
		}
		return new ArrayList<String>();
	}
	@Override
	public String toString() {
		return "TableMonitor [id=" + id + ", schema=" + schema + ", tableName="
				+ tableName + ", emails=" + emails + ", updatePeriod="
				+ updatePeriod + "]";
	}
}
