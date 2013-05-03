package controllers.modules.util;

import gov.nrel.util.TimeValue;
import gov.nrel.util.Utility;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import models.EntityUser;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.io.JsonStringEncoder;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;
import play.mvc.results.BadRequest;
import play.mvc.results.Result;
import play.mvc.results.Unauthorized;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.RequestBuilder;

import controllers.modules2.framework.ProductionModule;

public class ModuleStreamer {

	private static final Logger log = LoggerFactory
			.getLogger(ModuleStreamer.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	private static AsyncHttpClient client;

	private static String baseUrl;

	static {
		try {
			client = ProductionModule.createSingleton();

			baseUrl = (String) Play.configuration.get("application.requestUrl")
					+ "/api/";

		} catch (Exception e) {
			throw new RuntimeException("exception with http client", e);
		}
	}

	private static SSLContext initialize() throws NoSuchAlgorithmException,
			KeyStoreException, UnrecoverableKeyException,
			KeyManagementException {
		X509TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] xcs, String string)
					throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] xcs, String string)
					throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, new TrustManager[] { tm }, null);
		return ctx;
	}

	public static Info startStream(List<String> urls)
			throws InterruptedException, ExecutionException, IOException {
		return startStream(urls, "");
	}

	public static Info startStream(List<String> urls, String postUrl)
			throws InterruptedException, ExecutionException, IOException {
		return startStream(urls, postUrl, false);
	}

	public static Info startStream(List<String> urls, String postUrl,
			boolean forceRequest) throws InterruptedException,
			ExecutionException, IOException {

		// OKAY, at this point, we are ready to rock and roll
		Request request = Request.current();
		
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
				throw new Unauthorized(
					"Must supply basic auth username and key(key goes in password field)");
		}

		Info info;
		if (request.isNew || forceRequest) {
			OurPromise<Object> promise = new OurPromise<Object>();
			PipeCPromiseWriter writer = new PipeCPromiseWriter(promise, urls);
			CountDownLatch latch = new CountDownLatch(urls.size());
			request.args.put("time", System.currentTimeMillis());

			List<ListenableFuture<Object>> requestList = new ArrayList<ListenableFuture<Object>>();
			info = new Info(promise, latch, requestList);
			request.args.put("info", info);

			for (String url : urls) {

				// fix this so it is passed in instead....
				Realm realm = new Realm.RealmBuilder()
						.setPrincipal(username)
						.setPassword(password)
						.setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
						.build();


				String postUrl2 = url+postUrl;
				String fullUrl = formUrl(postUrl2);

				if (log.isInfoEnabled())
					log.info("sending request to url=" + fullUrl);
				RequestBuilder b = new RequestBuilder("GET").setUrl(fullUrl)
						.setRealm(realm);
				com.ning.http.client.Request httpReq = b.build();

				AsyncHandler<Object> handler = new PipeABytesToString(writer,
						url);
				ListenableFuture<Object> executeRequest = client.executeRequest(httpReq, handler);
				requestList.add(executeRequest);
				if (log.isInfoEnabled())
					log.info("sent request to url="+fullUrl);
			}
		} else {
			info = (Info) request.args.get("info");
		}

		return info;
	}

	private static String formUrl(String postUrl) {
		
		String temp = baseUrl;
		String fullUrl = temp + postUrl;
		
		//WE could do this better but for now it works...
		
		Request request = Request.current();
		String val = request.params.get("singleNode");
		boolean hasParamAlready = false;
		if("true".equalsIgnoreCase(val)) {
			hasParamAlready = true;
			fullUrl = request.getBase()+"/api/"+postUrl+"?singleNode=true";
		}
		
		String reverse = request.params.get("reverse");
		if("true".equalsIgnoreCase(reverse)) {
			if(hasParamAlready) {
				fullUrl += "&reverse=true";
			} else
				fullUrl += "?reverse=true";
			hasParamAlready = true;
		}
		return fullUrl;
	}

	public static void processResponses(Info info, Processor processor) {
		try {
			if(info.isInFailure())
				return; //no need to do anything as code below already ran on first important failure...
			if(log.isDebugEnabled())
				log.debug("firing processing responses");
			processResponsesImpl(info, processor);
		} catch(OldSuccessfulAbort e) { 
			//the last values almost always aborts after getting just enough data and uses SuccessfulAbort to do so
			if (log.isInfoEnabled())
				log.info("we successfully aborted and are closing the connection");
			cleanup(info);
		} catch(Result e) {
			if (log.isInfoEnabled())
				log.info("unauthorized, running cleanup now");
			info.setInFailure(true);
			cleanup(info);
			if (log.isInfoEnabled())
				log.info("cleanup complete");
			throw e;
		} catch (RuntimeException e) {
			if (log.isWarnEnabled())
        		log.warn("Exception occurred, about to cleanup", e);
			if (log.isInfoEnabled())
				log.info("about to cleanup");
			writeFooter(null, e);
//			Response response = Response.current();
//			response.writeChunk("{FAILURE processing.  exception="
//					+ e.getClass().getName() + " msg=" + e.getMessage()+" host="+UniqueKeyGenerator.getHostname()+"}");
			info.setInFailure(true);
			cleanup(info);
			if (log.isInfoEnabled())
				log.info("cleaned up now so no more writes");
			throw e;
		} finally {
			CountDownLatch latch = info.getLatch();
			if(log.isDebugEnabled())
				log.debug("done firing processing. latch count=" + latch.getCount());
		}
	}

	private static void cleanup(Info info) {
		exhaustTheLatch(info.getLatch());
		
		List<ListenableFuture<Object>> connections = info.getRequestList();
		for(ListenableFuture<Object> connection : connections) {
			try {
				connection.abort(null);
			} catch(Exception e) {
				if (log.isWarnEnabled())
	        		log.warn("trouble closing connection", e);
			}
		}
	}

	private static void processResponsesImpl(Info info,	Processor processor) {
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		List<Object> responses = promise.resetAndGetResponses();
		int jsonRowCountForDebug = 0;
		for (Object resp : responses) {
			if (resp instanceof String) {
				String s = (String) resp;
				if ("start".equals(s)) {
					if (log.isInfoEnabled())
						log.info("firing start");
					processor.onStart();
				} else {
					if (log.isInfoEnabled())
						log.info("firing what is this="+s);
					throw new RuntimeException("what is this=" + s);
				}
			} else if (resp instanceof Complete) {
				if (log.isInfoEnabled())
					log.info("firing complete to processor");
				Complete c = (Complete) resp;
				processor.complete(c.getUrl());
				latch.countDown();
			} else if (resp instanceof JsonRow) {
				jsonRowCountForDebug++;
				JsonRow row = (JsonRow) resp;
				if(log.isDebugEnabled())
					log.debug("ticket 42661899 - in ModuleStreamer.processResponsesImpl, we've been here "+jsonRowCountForDebug +" times.");
				try {
					processor.incomingJsonRow(row.isFirstChunk(), row);
				}
				catch (Exception e) {
					e.printStackTrace();
					for (ListenableFuture<Object> f:info.getRequestList()) {
						try {
							f.abort(null);
						} catch(Exception e2) {
							if (log.isWarnEnabled())
				        		log.warn("trouble closing connection", e);
						}
					}
					throw new RuntimeException(e);
				}
			} else if (resp instanceof Problem) {
				if (log.isInfoEnabled())
					log.info("firing problem to processor");
				Problem p = (Problem) resp;
				processor.onFailure(p.getUrl(), p.getException());
				// we exhaust the latch as we don't want to wait on giving the
				// user the response...
				throw new RuntimeException("Failured processing url="+p.getUrl()+".  exc="+p.getException().getClass().getName()+" msg="+p.getException().getMessage()+"");
			} else if (resp instanceof HttpBadResponse) {
				if (log.isInfoEnabled())
					log.info("firing httpBadResponse to processor");
				HttpBadResponse http = (HttpBadResponse) resp;

				String fullUrl = baseUrl+http.getUrl();
				int code = http.getStatus().getStatusCode();
				String statusText = http.getStatus().getStatusText();
				if (log.isInfoEnabled())
					log.info("[Failure when getting url=" + fullUrl
						+ " status code=" + code + " txt=" + statusText
						+ " msg=" + http.getBody() + "]");
				
				String msg = "[Failure when getting url=" + fullUrl
						+ " status code=" + code + " txt=" + statusText
						+ " msg=" + http.getBody() + "]";
				if (code == 401)
					throw new Unauthorized(msg);
				else if(code == 400)
					throw new BadRequest(msg);
				else if(code == 500)
					throw new Error(msg);
				
				Response response = Response.current();
				response.status = code;
				writeHeader(null);
				throw new RuntimeException("Failure when getting url="+http.getUrl()+" status code="+code+" txt="+statusText+" msg="+http.getBody()+"");
			} else {
				if (log.isInfoEnabled())
					log.info("resp not found, throwing exception");
				throw new RuntimeException("what is this="+resp);
			}
		}
	}

	private static void exhaustTheLatch(CountDownLatch latch) {
		while (latch.getCount() != 0) {
			latch.countDown();
		}
	}

	public static String translateToString(String s, TimeValue r) {
		try {
			s += mapper.writeValueAsString(r);
			return s;
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeHeader(String param) {
		Response response = Response.current();
		response.contentType = "text/plain";
		String lastPart = "{\"data\":[";
		if (param != null) {
			response.contentType = "text/javascript";
			lastPart = param + "(" + lastPart;
		}
		response.writeChunk(lastPart);
	}

	public static void writeFooter(String param, Throwable error) {
		Response response = Response.current();
		//it's possible the connection has been closed, if so, give up
		try {
			String footer = "";
			
			response.writeChunk("],");
			writeError(error);
			response.writeChunk("}");
			
			if (param != null) {
				response.writeChunk(footer += ");");
			}
		}
		catch (Exception e) {
			if (log.isInfoEnabled())
				log.info("can't write footer, connection is closed (or other error writing footer)");
		}
	}

	private static void writeError(Throwable error) {
		Response response = Response.current();
		StringBuffer errorBlock = new StringBuffer("\"error\":[");
		if (error != null) {
			errorBlock.append("\"");
			JsonStringEncoder inst = JsonStringEncoder.getInstance();
			errorBlock.append(inst.quoteAsString(""+error.getClass()+": "));
			errorBlock.append(inst.quoteAsString(""+error.getMessage()));
			String mode = Play.configuration.getProperty("application.mode");
			if(!"prod".equals(mode)) {
				inst = JsonStringEncoder.getInstance();
				errorBlock.append(inst.quoteAsString(""+ExceptionUtils.getStackTrace(error)));
			}
			errorBlock.append("\"");
		}
		errorBlock.append("]");
		response.writeChunk(errorBlock);
	}

}
