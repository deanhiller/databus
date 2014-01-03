package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jruby.exceptions.RaiseException;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.results.BadRequest;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class RelationalOperationProcessor extends PushOrPullProcessor {

	private static final Logger log = LoggerFactory.getLogger(RelationalOperationProcessor.class);
	private String toExecute = "";
	private CompiledScript script;
	private ScriptEngine engine;
	private RelationalContext relationalContext;
	private String resultingColumn;
	private Object resultingColumnForcedDataType;
	private String engineName = "javascript";
	
	private BigDecimal emptyBigDecimal = new BigDecimal(0);
	private BigInteger emptyBigInt = new BigInteger("0");
	private String emptyString = "";
	
	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
		String script = params.getParams().get(0);
		String lang = options.get("engineName");
		if (StringUtils.isNotBlank(lang)) 
			engineName = lang;
				
		ScriptEngineManager manager = new ScriptEngineManager();
		//com.sun.script.jython.JythonScriptEngineFactory f;
		com.sun.script.jruby.JRubyScriptEngine jrse;

		//System.out.println(" engines:   "+getSupportedEnginesString());
		engine = manager.getEngineByName(engineName);
		if (engine == null) {
			throw new BadRequest("The engineName you specified '"+engineName+"' is not available.  Supported scripting engines are " + getSupportedEnginesString()+".  An engineName can be any of the 'aliases' listed for an engine");
		}
		
		if (StringUtils.isEmpty(script))
			throw new BadRequest("Relational Module requires a script to run");
		
		try {
			initializeScript(script);
		}
		catch (ScriptException se) {
			throw new BadRequest("Your script is not legal");
		}
		relationalContext = new RelationalContext();
		return newPath;
	}
	
	private String getSupportedEnginesString() {
		String results = "";
		ScriptEngineManager mgr = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = mgr.getEngineFactories();

        for (ScriptEngineFactory factory : factories) {

            String engName = factory.getEngineName();
            String engVersion = factory.getEngineVersion();
            String langName = factory.getLanguageName();
            String langVersion = factory.getLanguageVersion();

            results += "\nScript Engine: "+engName+" ("+engVersion+"), Language: "+langName+", Language Version "+langVersion;

            List<String> engNames = factory.getNames();
            results+="Engine Aliases:";
            for(String name : engNames) {
                results+=name+", ";
            }
        }
        return results;
	}

	private void initializeScript(String scriptText) throws ScriptException {
		resultingColumn = StringUtils.substringBefore(scriptText, "=");
		if (StringUtils.contains(resultingColumn, "(")) {
			String dataTypeString = StringUtils.substringAfter(resultingColumn, "(");
			dataTypeString = StringUtils.substringBefore(dataTypeString, ")");
			resultingColumn = StringUtils.substringBefore(resultingColumn, "(");
			setResultingColumnForcedDataTypeFromShortString(dataTypeString);
		}
		toExecute = scriptText;
		if (!StringUtils.containsIgnoreCase(engineName, "python"))
			toExecute = StringUtils.substringAfter(scriptText, "=");
		toExecute=StringUtils.replace(toExecute, "÷", "/");
		//ScriptEngineManager manager = new ScriptEngineManager();
		//engine = manager.getEngineByName(engineName);
		if(!(engine instanceof Compilable)) {
			throw new BadRequest("the engine you specified "+engineName+" is not compilable!!  This is a configuration error in databus!");
		}
		script = ((Compilable) engine).compile(toExecute);
	}

	private void setResultingColumnForcedDataTypeFromShortString(
			String dataTypeString) {
		
		if (StringUtils.equals("BigDecimal", dataTypeString))
			resultingColumnForcedDataType = emptyBigDecimal;
		else if (StringUtils.equals("BigInteger", dataTypeString))
			resultingColumnForcedDataType = emptyBigDecimal;
		else if (StringUtils.equals("String", dataTypeString))
			resultingColumnForcedDataType = emptyString;
		else
			throw new BadRequest("unsupported data type "+dataTypeString+" only BigDecimal, BigInteger and String are allowed");

	}
	
	@Override
	public ReadResult read() {
		PullProcessor p = (PullProcessor) getSingleChild();

		ReadResult result = null;
		TSRelational row = relationalContext.getRowNum()>0?relationalContext.previousRow(0):null;
		do {
			//IF modifyRow returns null, it is because we are modifying the value out or cleaning it
			//and do not want upstream to see that result at all, so continue reading until row != null
			if (relationalContext.isComplete()) {
				result = p.read();
				if(result == null)
					return result;
				row = result.getRow();
			}

			//If row is null from downstream, just feed him upstream since it is probably an error or something
			if(row == null)
				return result;
			
			//IF row is null from this one, keep reading until we get data
			row = modifyRow(row);
			result.setRow(row);
		} while(row == null);
		
		return result;
	}

	@Override
	protected TSRelational modifyRow(TSRelational tv) {
		
		relationalContext.addPrevious(tv);
		//inside the loop so that the client code can't forget to reset it.
		//it'll always be true unless they explicitly set it or insert a row:
		relationalContext.setComplete(true);
		Bindings bindings = handleBindings(tv);
		Object result;
		try {
			if (StringUtils.containsIgnoreCase(engineName, "python") 
					//||StringUtils.containsIgnoreCase(engineName, "ruby")
					) {
				log.debug("executing the script: "+ toExecute);
				for (Entry<String, Object> entry:bindings.entrySet())
					log.debug("----"+entry.getKey()+":"+entry.getValue());
				script.eval(bindings);
				result = engine.get(resultingColumn);
				log.debug("after execution: ");
				for (Entry<String, Object> entry:bindings.entrySet())
					log.debug("----"+entry.getKey()+":"+entry.getValue());
			}
			else {
				log.debug("executing the script: "+ toExecute);
				for (Entry<String, Object> entry:bindings.entrySet())
					log.debug("----"+entry.getKey()+":"+entry.getValue());
				result = script.eval(bindings);
				log.debug("after execution result is '"+result+"': ");
				for (Entry<String, Object> entry:bindings.entrySet())
					log.debug("----"+entry.getKey()+":"+entry.getValue());
			}
		}
		catch (ScriptException e) {
			if (e.getCause() instanceof RaiseException)
				throw new BadRequest("Your script failed to evaluate for "+tv+" with exception "+((RaiseException)e.getCause()).getException());
			throw new BadRequest("Your script failed to evaluate for "+tv+" with exception "+ExceptionUtils.getFullStackTrace(e.getCause()));
		}
		if(resultingColumnForcedDataType == null && result instanceof Double) {
			result = new BigDecimal(result+"");
		}
		relationalContext.addPreviousReturned(tv);

		if (!relationalContext.isDiscardRow()) {
			tv.put(resultingColumn,  result);
			relationalContext.incRowNum();
		}
		else {
			relationalContext.setComplete(true);
			tv = null;
		}

		return tv;
	}

	private Bindings handleBindings(TSRelational tv) {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		String attrPrefix = "";
		if (StringUtils.containsIgnoreCase(engineName, "ruby"))
			attrPrefix = "@";
		bindings.put("relationalContext", relationalContext);
		if (StringUtils.containsIgnoreCase(engineName, "ruby")) {
			for (Entry<String, Object> entry:tv.entrySet()) {
				log.debug("binding "+attrPrefix+entry.getKey().toLowerCase()+":"+entry.getValue()+" type "+entry.getValue().getClass());
				bindings.put(attrPrefix+entry.getKey().toLowerCase(), entry.getValue());
			}
		}
		else {
			for (Entry<String, Object> entry:tv.entrySet()) {
				Object toBind = entry.getValue();
				log.debug("binding "+attrPrefix+entry.getKey()+":"+entry.getValue()+" type "+entry.getValue().getClass());
				if (StringUtils.containsIgnoreCase(engineName, "ython")) {
					if (entry.getValue() instanceof BigInteger) {
						toBind = new PyLong((BigInteger)entry.getValue());
					}
					if (entry.getValue() instanceof BigDecimal) {
						//TODO:  this ABSOLUTELY needs to change.  the call to .doubleValue() loses the precision granted by 
						//BigDecimal.  However, I can find no way to coerce a BigDecimal into jython without losing precision yet.
						toBind = new PyFloat(((BigDecimal)entry.getValue()).doubleValue());
					}
				}
				bindings.put(attrPrefix+entry.getKey(), toBind);
			}
		}
		if (!bindings.containsKey(resultingColumn))
			bindings.put(resultingColumn, null);
//		for (Entry<String, Object> e:bindings.entrySet())
//			System.out.println("bindings is  "+e.getKey()+":"+e.getValue());
		return bindings;

	}

}
