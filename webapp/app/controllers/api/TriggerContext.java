package controllers.api;

import java.util.Map;

public class TriggerContext {

	private Map<String, String> row;

	public TriggerContext(Map<String, String> jsonRow) {
		this.row = jsonRow;
	}

	public Map<String, String> getRow() {
		return row;
	}

	
}
