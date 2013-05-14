package controllers.modules2.framework;

import play.utils.FastRuntimeException;

public class SuccessfulAbort extends FastRuntimeException {

	private static final long serialVersionUID = 1L;
	private boolean retryImmediately;

	public SuccessfulAbort() {
	}

	public SuccessfulAbort(boolean retryImmediately) {
		this.retryImmediately = retryImmediately;
	}
	
	public SuccessfulAbort(String message) {
		super(message);
	}

	public SuccessfulAbort(String message, Throwable cause) {
		super(message, cause);
	}

	public SuccessfulAbort(boolean b, String message) {
		super(message);
		this.retryImmediately = b;
	}

	public boolean isRetryImmediately() {
		return retryImmediately;
	}
	
}
