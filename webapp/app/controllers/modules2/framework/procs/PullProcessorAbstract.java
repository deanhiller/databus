package controllers.modules2.framework.procs;

import java.util.HashMap;

import controllers.modules2.framework.Direction;


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
