package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.mvc.Http.Request;

import models.message.ChartVarMeta;
import models.message.StreamModule;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.DNegationProcessor;


public abstract class PullProcessorAbstract extends ProcessorSetupContainer implements PullProcessor {

	public void startEngine() {
		if(parent instanceof PullProcessor) {
			PullProcessor next = (PullProcessor) parent;
			next.startEngine();
		} else if(parent instanceof EngineProcessor) {
			EngineProcessor engine = (EngineProcessor) parent;
			engine.startEngine();
		}
	}
	public PullProcessor getChild() {
		return (PullProcessor) getSingleChild();
	}

	@Override
	public ReadResult read() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MetaInformation getGuiMeta() {
		//returning null means don't display this module in the GUI.  returning an empty hashmap
		//means display this module in the GUI and he has no parameters(hmmmmm, our wizard will actually show a blank page with a next button right now but oh well)
		return null;
	}
	
	@Override
	public Direction getSinkDirection() {
		return Direction.PULL;
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.PULL;
	}

	protected String fetchProperty(String key, String defaultVal, Map<String, String> options) {
		String s = options.get(key);
		if(s != null)
			return s;
		return defaultVal;
	}

	@Override
	public List<String> getAggregationList() {
		return parent.getAggregationList();
	}
}
