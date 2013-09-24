package controllers.modules2.framework.procs;

import java.util.HashMap;
import java.util.Map;

import models.message.ChartVarMeta;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ReadResult;


public abstract class PullProcessorAbstract extends ProcessorSetupAbstract implements PullProcessor {

	public void startEngine() {
		if(nextInChain instanceof PullProcessor) {
			PullProcessor next = (PullProcessor) nextInChain;
			next.startEngine();
		} else if(nextInChain instanceof EngineProcessor) {
			EngineProcessor engine = (EngineProcessor) nextInChain;
			engine.startEngine();
		}
	}
	public PullProcessor getChild() {
		return (PullProcessor) this.child;
	}

	@Override
	public ReadResult read() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ChartVarMeta> getParameterMeta() {
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

	public void setChild(ProcessorSetup mock) {
		this.child = mock;
	}
	
	protected String fetchProperty(String key, String defaultVal, HashMap<String, String> options) {
		String s = options.get(key);
		if(s != null)
			return s;
		return defaultVal;
	}
}
