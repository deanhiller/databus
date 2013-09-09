package controllers.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.message.PostTrigger;

public class TriggerContext {

	private static final Logger log = LoggerFactory.getLogger(TriggerContext.class);

	private Map<String, String> row;
	private PostTrigger trigger;

	public TriggerContext(Map<String, String> jsonRow, PostTrigger trigger) {
		this.row = jsonRow;
		this.trigger = trigger;
	}

	public Map<String, String> getRow() {
		return row;
	}

	public void fireCallback() {
		String callback = trigger.getCallback();
		
		
	}
}
