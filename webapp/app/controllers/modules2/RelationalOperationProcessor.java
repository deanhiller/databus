package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;

import play.mvc.results.BadRequest;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class RelationalOperationProcessor extends PushOrPullProcessor {

	private CompiledScript script;
	private ScriptEngine engine;
	private RelationalContext relationalContext;
	private String resultingColumn;
	private Object resultingColumnForcedDataType;
	
	private BigDecimal emptyBigDecimal = new BigDecimal(0);
	private BigInteger emptyBigInt = new BigInteger("0");
	private String emptyString = "";
	
	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		String script = params.getParams().get(0);
		
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
	
	private void initializeScript(String scriptText) throws ScriptException {
		resultingColumn = StringUtils.substringBefore(scriptText, "=");
		if (StringUtils.contains(resultingColumn, "(")) {
			String dataTypeString = StringUtils.substringAfter(resultingColumn, "(");
			dataTypeString = StringUtils.substringBefore(dataTypeString, ")");
			resultingColumn = StringUtils.substringBefore(resultingColumn, "(");
			setResultingColumnForcedDataTypeFromShortString(dataTypeString);
		}
		String toExecute = StringUtils.substringAfter(scriptText, "=");
		toExecute=StringUtils.replace(toExecute, "÷", "/");
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("javascript");
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
	protected TSRelational modifyRow(TSRelational tv) {
		relationalContext.addPrevious(tv);
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("relationalContext", relationalContext);
		for (Entry<String, Object> entry:tv.entrySet())
			bindings.put(entry.getKey(), entry.getValue());
		if (!bindings.containsKey(resultingColumn))
			bindings.put(resultingColumn, resultingColumnForcedDataType);
		Object result;
		try {
			result = script.eval();
		} catch (ScriptException e) {
			throw new BadRequest("Your script failed to evaluate for "+tv+" with exception "+e);
		}
		tv.put(resultingColumn,  result);
		relationalContext.incRowNum();
		return tv;
	}

}
