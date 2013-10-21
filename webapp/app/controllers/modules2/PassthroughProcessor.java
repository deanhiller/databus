package controllers.modules2;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.PullProcessorAbstract;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class PassthroughProcessor extends PullProcessorAbstract {
	@Override
	public ReadResult read() {
		return getChild().read();
	}
}
