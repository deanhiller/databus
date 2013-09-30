package controllers.modules2.framework.procs;

import java.util.Map;

import models.message.ChartVarMeta;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;


public abstract class PushOrPullProcessor extends PushProcessorAbstract implements PullProcessor {

	@Override
	public Map<String, ChartVarMeta> getParameterMeta() {
		//returning null means don't display this module in the GUI.  returning an empty hashmap
		//means display this module in the GUI and he has no parameters(hmmmmm, our wizard will actually show a blank page with a next button right now but oh well)
		return null;
	}

	public void startEngine() {
		if(parent instanceof PullProcessor) {
			PullProcessor next = (PullProcessor) parent;
			next.startEngine();
		} else if(parent instanceof EngineProcessor) {
			EngineProcessor engine = (EngineProcessor) parent;
			engine.startEngine();
		}
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		TSRelational newRow = modifyRow(row);
		if(newRow != null)
			super.incomingChunk(url, newRow, flag);
	}

	protected abstract TSRelational modifyRow(TSRelational row);

	@Override
	public ReadResult read() {
		PullProcessor p = (PullProcessor) child;

		ReadResult result;
		TSRelational row;
		do {
			//IF modifyRow returns null, it is because we are modifying the value out or cleaning it
			//and do not want upstream to see that result at all, so continue reading until row != null
			result = p.read();
			if(result == null)
				return result;

			row = result.getRow();
			//If row is null from downstream, just feed him upstream since it is probably an error or something
			if(row == null)
				return result;
			
			//IF row is null from this one, keep reading until we get data
			row = modifyRow(row);
			result.setRow(row);
		} while(row == null);
		
		return result;
	}
	
	@Override
	public Direction getSinkDirection() {
		//our direction is determined by our parents direction(since we do either way)
		return parent.getSinkDirection();
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.EITHER;
	}
}
