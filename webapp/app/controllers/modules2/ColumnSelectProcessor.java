package controllers.modules2;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;

public class ColumnSelectProcessor extends PushOrPullProcessor {
	
	//column to (optional)alias map
	private Map<String,String> colMap;
	
	@Override
	protected int getNumParams() {
		return 1;
	}
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
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
		TSRelational newTv = new TSRelational();
		for (Entry<String, String> col:colMap.entrySet()) {
			newTv.put(col.getValue(), tv.get(col.getKey()));
		}
		return newTv;
	}

}
