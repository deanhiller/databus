package robot;
import gov.nrel.util.StartupDetailed;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import models.message.RegisterResponseMessage;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utility {

	private final static Logger log = LoggerFactory.getLogger(Utility.class);
	
	public static BasicHttpContext setupPreEmptiveBasicAuth(HttpHost targetHost,
			DefaultHttpClient httpclient, String user, String apiKey) {
		httpclient.getCredentialsProvider().setCredentials(
		        new AuthScope(targetHost.getHostName(), targetHost.getPort()), 
		        new UsernamePasswordCredentials(user, apiKey));
	
		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);
	
		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}
	public static String sendRequest(DefaultHttpClient httpclient, String requestUri, String user, String apiKey) throws IOException,
	ClientProtocolException {
		return sendRequest(httpclient, requestUri, user, apiKey, 200);
	}
	public static String sendRequest(DefaultHttpClient httpclient, String requestUri, String user, String apiKey, int expectedCode) throws IOException,
			ClientProtocolException {
		int port = Utility.retrievePlayServerPort();
		HttpHost targetHost = new HttpHost("localhost", port, "http"); 
		BasicHttpContext localcontext = setupPreEmptiveBasicAuth(targetHost, httpclient, user, apiKey);
		
		log.info("trying to hit url="+requestUri);
		HttpGet httpget = new HttpGet(requestUri);
		HttpResponse response = httpclient.execute(targetHost, httpget, localcontext);
		
		HttpEntity entity = response.getEntity();
		
		InputStream instream = entity.getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(instream, writer);
		String theString = writer.toString();
		
		log.info("resp="+response.getStatusLine()+" body="+theString);
		Assert.assertEquals(expectedCode, response.getStatusLine().getStatusCode());
		
		return theString;
	}

	public static String sendPostRequest(DefaultHttpClient httpclient, String requestUri, String body, String user, String key) {
		try {
			return sendPostRequestImpl(httpclient, requestUri, body, user, key, 200);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String sendPostRequestExpectedToFail(DefaultHttpClient httpclient, String requestUri, String body, String user, String key) {
		try {
			return sendPostRequestImpl(httpclient, requestUri, body, user, key, 400);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String sendPostRequestImpl(DefaultHttpClient httpclient, String requestUri, String body, String user, String key,
				int expectedCode) throws ClientProtocolException, IOException {
		int port = Utility.retrievePlayServerPort();
		HttpHost targetHost = new HttpHost("localhost", port, "http"); 
		BasicHttpContext localcontext = setupPreEmptiveBasicAuth(targetHost, httpclient, user, key);
		
		HttpPost httpPost = new HttpPost(requestUri);
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(body));
		HttpResponse response2 = httpclient.execute(targetHost, httpPost, localcontext);

		//read out the body so we can re-use the client
		HttpEntity entity = response2.getEntity();
		InputStream in = entity.getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(in, writer);
		String theString = writer.toString();
		
		log.info("resp="+response2.getStatusLine()+" body="+theString);
		Assert.assertEquals(expectedCode, response2.getStatusLine().getStatusCode());
		
		return theString;
	}
	
	public static int retrievePlayServerPort() {
		// Grab the environment variable for the port. If its not found use 9000
		int port = 9000;
		String portString = System.getenv("SDIBUILD_PLAY_PORT");
		if (portString != null) {
			try {
				port = Integer.parseInt(portString);

				if ((port < 1000) || (port > 65530)) {
					port = 9000;
					log.warn("Utility.sendRequest() Warning: Port value out of range for Play Server port (1000 < port < 65530).  Using 9000 as default.");
				}
			} catch (Exception e) {
				log.warn("Utility.sendRequest() Exception: Unable to parse environment variable for Play Server port.  Using 9000 as default.");
			}
		} else {
			log.warn("Utility.sendRequest() Warning: Unable to find environment variable for Play Server port.  Using 9000 as default.");
		}

		return port;
	}
}
