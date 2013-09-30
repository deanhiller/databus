package controllers.modules2.framework.procs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.http.HttpStatus;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

public abstract class PushProcessorAbstract extends ProcessorSetupAbstract implements PushProcessor {

	@Override
	public Direction getSinkDirection() {
		return Direction.PUSH;
	}
	@Override
	public Direction getSourceDirection() {
		return Direction.PUSH;
	}

	protected PushProcessor getNextInChain() {
		return (PushProcessor) parent;
	}
	
	@Override
	public void onStart(String url, HttpStatus status) {
		getNextInChain().onStart(url, status);
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		getNextInChain().incomingChunk(url, row, flag);
	}
	
	@Override
	public void complete(String url) {
		getNextInChain().complete(url);
	}
	
	
	@Override
	public void addMissingData(String url, String errorMsg) {
		getNextInChain().addMissingData(url, errorMsg);
	}

	@Override
	public void onFailure(String url, Throwable exception, String errorMsg) {
		getNextInChain().onFailure(url, exception, errorMsg);
	}
	
}
