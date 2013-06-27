package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;

import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;

import play.mvc.results.BadRequest;

import controllers.modules.SplinesBasic;
import controllers.modules.SplinesLimitDerivative;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class ColumnSelectProcessor extends PushOrPullProcessor {
	
	//column to (optional)alias map
	private Map<String,String> colMap;
	
	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		String cols = params.getParams().get(0);
		colMap = new HashMap<String,String>();
		for (String s:StringUtils.split(cols, ",")) {
			String col=StringUtils.substringBefore(s, " as ");
			String alias=StringUtils.substringAfter(s, " as ");			
			if (StringUtils.isEmpty(alias))
				alias = col;
			colMap.put(col.trim(), alias.trim());
		}
		
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational tv) {
		TSRelational newTv = new TSRelational(timeColumn, valueColumn);
		for (Entry<String, String> col:colMap.entrySet()) {
			newTv.put(col.getValue(), tv.get(col.getKey()));
		}
		return newTv;
	}

}
