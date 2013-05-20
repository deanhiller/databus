package controllers.modules2.framework.procs;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;


public abstract class PushOrPullProcessor extends PushProcessorAbstract implements PullProcessor {

	public void startEngine() {
		if(nextInChain instanceof PullProcessor) {
			PullProcessor next = (PullProcessor) nextInChain;
			next.startEngine();
		} else if(nextInChain instanceof EngineProcessor) {
			EngineProcessor engine = (EngineProcessor) nextInChain;
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
		return nextInChain.getSinkDirection();
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.EITHER;
	}
}
