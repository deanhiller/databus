package controllers.auth;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super();
	}

	public InvalidCredentialsException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public InvalidCredentialsException(String arg0) {
		super(arg0);
	}

	public InvalidCredentialsException(Throwable arg0) {
		super(arg0);
	}

}
