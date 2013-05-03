package controllers.modules2.framework.http;

import controllers.modules2.framework.procs.PushProcessor;

public interface HttpListener {

	public void setProcessor(PushProcessor processor);
	
	public void onStatus(HttpStatus status);

	public void onBodyPartReceived(HttpBodyPart part);

	public void onCompleted(HttpCompleted completed);

	public void onThrowable(HttpException httpExc);
}
