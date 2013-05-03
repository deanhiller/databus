package controllers.modules2;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class PassthroughProcessor extends PushProcessorAbstract {
	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		getNextInChain().incomingChunk(url, row, flag);
	}
}
