package controllers.modules2.framework;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

import play.Play;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;

import controllers.ApiPostDataPoints;
import controllers.modules2.framework.translate.JsonToValuesFactory;
import controllers.modules2.framework.translate.TranslationFactory;

public class ProductionModule implements Module {

	private static AsyncHttpClient httpClient;
	private RawProcessorFactory factory;

	public ProductionModule(RawProcessorFactory factory) {
		this.factory = factory;
	}

	@Override
	public void configure(Binder binder) {
		Config config = new Config();
		if(Play.configuration != null) {
			String baseUrl = (String) Play.configuration.get("application.requestUrl") + "/api/";
			config.setBaseUrl(baseUrl);
		}

		Feature useBigDecimalForFloats = DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS;
		Feature useBigIntegerForInts = DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS;
		
		
		ObjectMapper m = new ObjectMapper();
		m.configure(useBigDecimalForFloats, true);
		m.configure(useBigIntegerForInts, true);
		m.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);

		binder.bind(ObjectMapper.class).toInstance(m);
		binder.bind(TranslationFactory.class).annotatedWith(Names.named("json")).to(JsonToValuesFactory.class).asEagerSingleton();
		
		//binder.bind(ProcessorSetup.class).toProvider(ProcessorFactory.class);
		binder.bind(RawProcessorFactory.class).toInstance(factory);
		
//		TypeLiteral<String> stringLit = new TypeLiteral<String>() {;};
//		TypeLiteral<Provider<Processor>> list = new TypeLiteral<Provider<Processor>>() {;};
//		MapBinder<String, Provider<Processor>> mapbinder2 = MapBinder.newMapBinder(binder, stringLit, list);
//		mapbinder2.addBinding("splineV1Beta").toProvider(new TypeLiteral<Provider<SplineProcessor>>() {;});
//		mapbinder2.addBinding("splineV1Beta").to(SplineProcessor.class);
//		mapbinder2.addBinding("invertV1Beta").to(InvertProcessor.class);

//		MapBinder<String, Processor> mapbinder = MapBinder.newMapBinder(binder, String.class, Processor.class);
//		mapbinder.addBinding("splineV1Beta").to(SplineProcessor.class);
//		mapbinder.addBinding("invertV1Beta").to(InvertProcessor.class);

		binder.bind(AsyncHttpClient.class).toInstance(createSingleton());
		binder.bind(Config.class).toInstance(config);
	}

	public synchronized static AsyncHttpClient createSingleton() {
		try {
			if(httpClient != null)
				return httpClient;
			
			Builder builder = new AsyncHttpClientConfig.Builder();
			SSLContext ctx = initialize();

			//ExecutorService execService = Executors.newFixedThreadPool(20);
			builder.setCompressionEnabled(true).setAllowPoolingConnection(true)
					.setAllowSslConnectionPool(true)
					.setRequestTimeoutInMs(121000)
					.setMaxRequestRetry(0)
					.setMaximumConnectionsPerHost(1000).setSSLContext(ctx)
					.build();

			httpClient = new AsyncHttpClient(builder.build());
			return httpClient;
		} catch (Exception e) {
			throw new RuntimeException("exception with http client", e);
		}
	}

	private static SSLContext initialize() throws NoSuchAlgorithmException,
			KeyStoreException, UnrecoverableKeyException,
			KeyManagementException {
		X509TrustManager tm = new MyTrustMgr();

		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, new TrustManager[] { tm }, null);
		return ctx;
	}
	
	public static class MyTrustMgr implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] xcs, String string)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] xcs, String string)
				throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
