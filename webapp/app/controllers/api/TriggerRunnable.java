package controllers.api;

import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.playorm.cron.impl.HashGenerator;
import org.playorm.cron.impl.HashGeneratorImpl;
import org.playorm.cron.impl.db.WebNodeDbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.results.BadRequest;

import models.message.PostTrigger;

public class TriggerRunnable implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(TriggerRunnable.class);
	private PostTrigger trigger;
	private Map<String, String> jsonRow;
	private int upNodeCount;
	private HashGenerator generator = new HashGeneratorImpl();
	private List<WebNodeDbo> nodes;
	
	public TriggerRunnable(PostTrigger trig, Map<String, String> jsonRow2, List<WebNodeDbo> nodes, int upNodeCount) {
		this.jsonRow = jsonRow2;
		this.trigger = trig;
		this.upNodeCount = upNodeCount;
		this.nodes = nodes;
	}

	@Override
	public void run() {
		try {
			runImpl();
		} catch(Exception e) {
			log.warn("trigger failed to run. db="+trigger.getDatabase()+" table="+trigger.getTable(), e);
		}
	}
	
	public void runImpl() throws ScriptException, NoSuchMethodException {
		//BIG NOTE: We should change this eventually to forward the trigger request to the node for this trigger so we
		//can implement caching on a scalable level such that each node does not have that large of a cache resulting in many cache hits
		//step 1. hash the table name!!
		int hash = trigger.getTable().hashCode();
		//step 2. reduce the range of hashcodes to be an index from 0 to upNodeCount
		int index = generator.generate(hash, upNodeCount);
		//step 3. lookup the node we should be sending the request to(this node would have a cache for just these tables and the tables of a failed node)
		WebNodeDbo node = nodes.get(index);
		//step 4. fire an asynchronous request to the node that deals with this trigger(on failure, perhaps fire to one of the other nodes responsible)
		

		//NOTE: for now, the above code is useless and we just run the stuff ourselves until we have issues...ie. speed of dev is more important right now then
		//getting this perfect		
		execute();
	}

	public void execute() throws ScriptException, NoSuchMethodException {
		String script = trigger.getScript();
		TriggerContext ctx = new TriggerContext(jsonRow, trigger);
		ScriptEngine engine;
		if("javascript".equals(trigger.getScriptLanguage())) {
			ScriptEngineManager manager = new ScriptEngineManager();
			engine = manager.getEngineByName("javascript");
		} else {
			throw new RuntimeException("we do not run this language yet="+trigger.getScriptLanguage()+" triggerdb="+trigger.getDatabase()+" table="+trigger.getTable());
		}

		engine.eval(script);
		Invocable inv = (Invocable) engine;
        // invoke the global function named "trigger"
        inv.invokeFunction("trigger", ctx );
	}

}
