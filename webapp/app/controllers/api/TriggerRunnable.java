package controllers.api;

import java.util.Map;

public class TriggerRunnable implements Runnable {

	private String script;
	private String language;
	private Map<String, String> jsonRow;

	public TriggerRunnable(String script, String language, Map<String, String> jsonRow) {
		this.script = script;
		this.language = language;
		this.jsonRow = jsonRow;
	}

	@Override
	public void run() {
		
	}

}
