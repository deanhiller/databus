package controllers.modules2;

import java.util.Map;

import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;

public class DynamicAggregationPipeProcessor extends AggregationPipeProcessor {

	@Override
	protected int getNumParams() {
		return -1;
	}
	
	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		setDynamic(true);
		
		return super.init(path, nextInChain, visitor, options);
	}
}
