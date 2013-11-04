package controllers.modules2.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.message.StreamModule;

import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.ProcessorSetupAbstract;


/** 
 * Marker interface
 * @author dhiller2
 *
 */
public abstract class EndOfChain extends ProcessorSetupAbstract {

	protected ProcessorSetup parent;

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		this.parent = nextInChain;
		return super.init(path, nextInChain, visitor, options);
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup child, boolean alreadyAdded) {
		return null;
	}
	
	@Override
	public void createTree(ProcessorSetup parent, StreamModule info, VisitorInfo visitor) {
		this.parent = parent;
	}

	@Override
	public List<String> getAggregationList() {
		return new ArrayList<String>();
	}

}
