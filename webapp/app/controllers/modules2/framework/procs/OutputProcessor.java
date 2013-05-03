package controllers.modules2.framework.procs;

public interface OutputProcessor extends PushProcessor {

	void setChunkSize(int size);

	void setCallbackParam(String callbackParam);

}
