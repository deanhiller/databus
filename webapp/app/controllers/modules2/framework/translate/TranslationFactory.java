package controllers.modules2.framework.translate;

import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.PushProcessor;

public interface TranslationFactory {

	public HttpListener get(PushProcessor processor);

}
