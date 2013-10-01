package controllers.modules.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.message.ChartVarMeta;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class MockProcessor extends PushProcessorAbstract implements PullProcessor{

	private List<TSRelational> rows = new ArrayList<TSRelational>();
	
	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		rows.add(row);
	}

	@Override
	public MetaInformation getGuiMeta() {
		//returning null means don't display this module in the GUI.  returning an empty hashmap
		//means display this module in the GUI and he has no parameters(hmmmmm, our wizard will actually show a blank page with a next button right now but oh well)
		return null;
	}

	@Override
	public void complete(String url) {
	}

	public List<TSRelational> getRows() {
		return rows;
	}

	@Override
	public ReadResult read() {
		if(rows.size() == 0)
			return new ReadResult();
		TSRelational row = rows.remove(0);
		return new ReadResult(null, row);
	}

	@Override
	public void startEngine() {
	}

	public void setRows(List<TSRelational> rows2) {
		this.rows = rows2;
	}
	
}
