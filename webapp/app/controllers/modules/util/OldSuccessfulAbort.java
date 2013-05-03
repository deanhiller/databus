package controllers.modules.util;

import play.exceptions.PlayException;
import play.utils.FastRuntimeException;

public class OldSuccessfulAbort extends FastRuntimeException {

	public OldSuccessfulAbort() {
	}

	public OldSuccessfulAbort(String message) {
		super(message);
	}

	public OldSuccessfulAbort(String message, Throwable cause) {
		super(message, cause);
	}
}
