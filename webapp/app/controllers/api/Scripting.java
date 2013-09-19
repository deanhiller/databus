package controllers.api;

import java.util.HashMap;
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

public class Scripting {

	public static void main(String[] args) throws ScriptException, NoSuchMethodException {
		Map<String, String> jsonRow = new HashMap<String, String>();
		jsonRow.put("temp", "56");
		TriggerContext ctx = new TriggerContext(jsonRow , null);

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");
		engine.eval("function trigger(context) { var t = context.getRow(); var val = t.get('temp'); return true;}");
		Invocable inv = (Invocable) engine;
        // invoke the global function named "hello"
        Object result = inv.invokeFunction("trigger", ctx );
		System.out.println("result="+result);
		System.out.println("type="+result.getClass());
	}
}
