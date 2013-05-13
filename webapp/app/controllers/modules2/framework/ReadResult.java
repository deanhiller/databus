package controllers.modules2.framework;


public class ReadResult {

	private String errorMsg;
	private TSRelational row;
	private boolean isEndOfStream;
	private String url;
	private boolean missingData;
	private Throwable exception;

	
	@Override
	public String toString() {
		return "RES["+row+"]";
	}

	public ReadResult(String url2, Throwable exception, String errorMsg2) {
		this.exception = exception;
	}
	
	public ReadResult(String url, TSRelational tv) {
		this.url = url;
		this.row = tv;
	}

	public ReadResult() {
		isEndOfStream = true;
	}

	public ReadResult(String url2, String errorMsg2) {
		missingData = true;
		this.url = url2;
		this.errorMsg = errorMsg2;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public TSRelational getRow() {
		return row;
	}

	public String getUrl() {
		return url;
	}

	public boolean isEndOfStream() {
		return isEndOfStream;
	}

	public boolean isMissingData() {
		return missingData;
	}

	public Throwable getException() {
		return exception;
	}

	public void setRow(TSRelational row2) {
		this.row = row2;
	}
}
