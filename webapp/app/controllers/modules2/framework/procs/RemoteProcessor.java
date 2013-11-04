package controllers.modules2.framework.procs;

import gov.nrel.util.Utility;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import models.EntityUser;
import models.message.StreamModule;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
import play.mvc.results.Unauthorized;

import com.google.inject.Provider;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.RequestBuilder;

import controllers.modules2.framework.Config;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.AHttpChunkingListener;
import controllers.modules2.framework.chain.DNegationProcessor;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.translate.TranslationFactory;

public class RemoteProcessor extends EndOfChain {

	private static final Logger log = LoggerFactory.getLogger(RemoteProcessor.class);

	@Inject
	private AsyncHttpClient client;
	@Inject
	private Config config;
	@Inject
	private Provider<AHttpChunkingListener> listeners;

	private String path;
	
	@Override
	public void start(VisitorInfo visitor) {
		visitor.incrementStreamCount();
		String prevPath = params.getPreviousPath();
		sendHttpRequest(prevPath, visitor);
	}

	private void sendHttpRequest(String url, VisitorInfo visitor) {
		Request request = Request.current();
		AuthInfo auth = fetchAuthInfo(request);
		// fix this so it is passed in instead....
		Realm realm = new Realm.RealmBuilder()
				.setPrincipal(auth.getUsername())
				.setPassword(auth.getPassword())
				.setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
				.build();

		String fullUrl = formUrl(url);

		if (log.isInfoEnabled())
			log.info("sending request to url=" + fullUrl);
		RequestBuilder b = new RequestBuilder("GET").setUrl(fullUrl)
				.setRealm(realm);
		com.ning.http.client.Request httpReq = b.build();

		TranslationFactory factory = visitor.getTranslator();
		HttpListener listener = factory.get((PushProcessor) parent);
		
		AHttpChunkingListener handler = listeners.get();
		handler.setPromise(visitor.getPromise());
		handler.setUrl(url);
		handler.setListener(listener);
		
		try {
			int maxRetry = client.getConfig().getMaxRequestRetry();
			ListenableFuture<Object> future2 = client.executeRequest(httpReq, handler);
			List<ListenableFuture<Object>> list = visitor.getRequestList();
			list.add(future2);
		} catch(IOException e) {
			throw new RuntimeException("Failed to send request. url="+url);
		}
	}

	private AuthInfo fetchAuthInfo(Request request) {
		EntityUser user = Utility.getCurrentUser(Session.current());
		String username = request.user;
		String password = request.password;

		if (user != null && StringUtils.isBlank(username)) {
			username = user.getUsername();
			password = user.getApiKey();
			if (log.isInfoEnabled())
				log.info("user making request=" + username + " with apikey=" + password);
		}
		else {
			if (log.isInfoEnabled())
				log.info("user making request=" + username + " with basic auth password");
			if (request.user == null || request.user.length() == 0)
				throw new Unauthorized("Must supply basic auth username and key(key goes in password field)");
		}
		AuthInfo auth = new AuthInfo(username, password);
		return auth;
	}
	
	private String formUrl(String postUrl) {
		String temp = config.getBaseUrl();
		String fullUrl = temp + postUrl;
		
		Request request = Request.current();
		String val = request.params.get("singleNode");
		if("true".equalsIgnoreCase(val))
			fullUrl = request.getBase()+"/api/"+postUrl;
		Params params = request.params;
		params.remove("path");
		String urlEncode = params.urlEncode();
		
		return fullUrl + "?"+urlEncode;
	}

	public static class AuthInfo {

		private String username;
		private String password;

		public AuthInfo(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
		
	}

	@Override
	public Direction getSinkDirection() {
		return Direction.NONE;
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.PUSH;
	}

}
