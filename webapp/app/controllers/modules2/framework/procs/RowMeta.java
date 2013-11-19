package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.List;

public class RowMeta {

	private String timeColumn;
	private List<String> valueColumns;

	public RowMeta(String timeColumn, String valueColumn) {
		super();
		this.timeColumn = timeColumn;
		valueColumns = new ArrayList<String>();
		valueColumns.add(valueColumn);
	}
	
	public RowMeta(String timeColumn2, List<String> names) {
		this.timeColumn = timeColumn2;
		this.valueColumns = names;
	}

	public String getTimeColumn() {
		return timeColumn;
	}
	public String getValueColumn() {
		if(valueColumns.size() > 0)
			return valueColumns.get(0);
		return null;
	}
	public List<String> getValueColumns() {
		return valueColumns;
	}
}
