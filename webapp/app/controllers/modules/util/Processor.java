package controllers.modules.util;

import play.libs.F.Promise;

public interface Processor {

	void complete(String url);

	void incomingJsonRow(boolean isFirstChunk, JsonRow chunk);

	void onStart();

	void onFailure(String url, Throwable exception);

}
