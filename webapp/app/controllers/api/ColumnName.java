package controllers.api;

public class ColumnName {

	private long time;
	private String name;

	public ColumnName(long time, String colName) {
		this.time = time;
		this.name = colName;
	}

	public long getTime() {
		return time;
	}

	public String getName() {
		return name;
	}
	
}
