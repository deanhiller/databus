package controllers.modules2.framework.procs;

public class RowMeta {

	private String timeColumn;
	private String valueColumn;

	public RowMeta(String timeColumn, String valueColumn) {
		super();
		this.timeColumn = timeColumn;
		this.valueColumn = valueColumn;
	}
	
	public String getTimeColumn() {
		return timeColumn;
	}
	public String getValueColumn() {
		return valueColumn;
	}
}
