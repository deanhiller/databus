package controllers.modules2.framework;

import play.utils.FastRuntimeException;

public class SuccessfulAbort extends FastRuntimeException {

	private static final long serialVersionUID = 1L;

	public SuccessfulAbort() {
	}

	public SuccessfulAbort(String message) {
		super(message);
	}

	public SuccessfulAbort(String message, Throwable cause) {
		super(message, cause);
	}
}
