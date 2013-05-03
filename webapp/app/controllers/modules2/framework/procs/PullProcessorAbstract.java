package controllers.modules2.framework.procs;

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

}
