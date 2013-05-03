package controllers.modules2.framework.procs;

import controllers.modules2.framework.ReadResult;


public interface PullProcessor extends ProcessorSetup {

	ReadResult read();

	void startEngine();

}
