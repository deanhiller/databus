package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.http.HttpStatus;

public class BufferNode extends ProcessorSetupAbstract implements PullProcessor, PushProcessor {

	private static final Logger log = LoggerFactory.getLogger(BufferNode.class);
	private List<ReadResult> buffer = new ArrayList<ReadResult>();
	private ProcessedFlag flag;
	private int errorCount = 0;
	private boolean complete;
	
	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, HashMap<String, String> options) {
		super.init(path, nextInChain, visitor, options);
		return path;
	}

	@Override
	public ReadResult read() {
		if(buffer.size() == 0) {
			if(!complete) {
				if (log.isInfoEnabled())
					log.info(this+"buffer is empty");
				return null;
			}
			return new ReadResult();
		}
		ReadResult result = buffer.remove(0);
		if(result.getException() != null)
			throw new RuntimeException(result.getException().getMessage(), result.getException());
		
		if(buffer.size() < 50 && flag != null) {
			flag.setSocketRead(true);
		}
		return result;
	}

	@Override
	public void onStart(String url, HttpStatus status) {
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		ReadResult res = new ReadResult(url, row);
		addToBufferAndKick(res);

		checkBufferMaxedOut(flag);
	}

	private void addToBufferAndKick(ReadResult res) {
		buffer.add(res);
		
		PullProcessor pull = (PullProcessor) nextInChain;
		pull.startEngine();
	}

	private void checkBufferMaxedOut(ProcessedFlag flag) {
		if(buffer.size() >= 200) {
			if (log.isInfoEnabled())
				log.info(this+"set buffer off, more than 200");
			flag.setSocketRead(false);
		}
		this.flag = flag;
	}

	@Override
	public void complete(String url) {
		complete = true;
		ReadResult res = new ReadResult();
		addToBufferAndKick(res);
	}

	@Override
	public void addMissingData(String url, String errorMsg) {
		if(errorCount > 5)
			return;
		errorCount++;
		ReadResult res = new ReadResult(url, errorMsg);
		addToBufferAndKick(res);
	}

	@Override
	public void onFailure(String url, Throwable exception, String errorMsg) {
		if(errorCount > 5) //let's not cause out of memory as if we get tons of errors, just drop the rest(only first one matters)
			return; 
		errorCount++;
		ReadResult res = new ReadResult(url, exception, errorMsg);
		addToBufferAndKick(res);
	}

	@Override
	public Direction getSinkDirection() {
		return Direction.PUSH;
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.PULL;
	}

	@Override
	public void startEngine() {
		throw new UnsupportedOperationException("bug, should never be called");
	}

}
