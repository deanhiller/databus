package controllers.modules2.framework;

public interface ProcessedFlag {

	/**
	 * When called, this releases the upstream to start reading more data.  Until you call this, you are choking 
	 * the upstream to force it to slow down.
	 */
	public void setSocketRead(boolean on);
	
}
