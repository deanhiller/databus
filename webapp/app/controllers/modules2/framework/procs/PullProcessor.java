package controllers.modules2.framework.procs;

import java.util.Map;

import models.message.ChartVarMeta;
import controllers.modules2.framework.ReadResult;


public interface PullProcessor extends ProcessorSetup {

	ReadResult read();

	void startEngine();

	MetaInformation getGuiMeta();
}
