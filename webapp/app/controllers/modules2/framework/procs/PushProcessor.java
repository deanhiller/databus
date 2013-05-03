package controllers.modules2.framework.procs;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.http.HttpStatus;

public interface PushProcessor extends ProcessorSetup {
	
	void onStart(String url, HttpStatus status);

	/**
	 * Returns whether we can continue accepting data
	 *
	 * @param url
	 * @param row
	 * @param flag
	 * @return
	 */
	void incomingChunk(String url, TSRelational row, ProcessedFlag flag);

	void complete(String url);
	
	void addMissingData(String url, String errorMsg);
	
	void onFailure(String url, Throwable exception, String errorMsg);

}
