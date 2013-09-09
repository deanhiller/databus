package controllers.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.message.PostTrigger;

public class TriggerRunnable implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(TriggerRunnable.class);
	private PostTrigger trigger;
	private Map<String, String> jsonRow;

	public TriggerRunnable(PostTrigger trig, Map<String, String> jsonRow2) {
		this.trigger = trig;
	}

	@Override
	public void run() {
		TriggerContext ctx = new TriggerContext(jsonRow);
		if("javascript".equals(trigger.getScriptLanguage())) {
			
		} else
			log.info("we do not run this language yet="+trigger.getScriptLanguage()+" triggerdb="+trigger.getDatabase()+" table="+trigger.getTable());
	}

}
