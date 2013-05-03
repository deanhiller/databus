package controllers.modules2.framework.translate;

import javax.inject.Inject;
import javax.inject.Provider;

import controllers.modules2.framework.chain.CTranslatorJsonToValues;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.PushProcessor;

public class JsonToValuesFactory implements TranslationFactory {

	@Inject
	private Provider<CTranslatorJsonToValues> translators;
	
	@Override
	public HttpListener get(PushProcessor processor) {
		CTranslatorJsonToValues translator = translators.get();
		translator.setProcessor(processor);
		return translator;
	}

}
