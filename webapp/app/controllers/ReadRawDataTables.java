package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import models.message.RegisterAggregation;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.BasicHttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.Counter;

public class ReadRawDataTables {

	private static final Logger log = LoggerFactory.getLogger(ReadRawDataTables.class);
	/**
	 * @param args
	 * @throws IOException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws UnrecoverableKeyException 
	 */
	public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		log.info("starting up");
		new ReadRawDataTables().start();
	}

	private DefaultHttpClient httpclient;
	private BasicHttpContext localcontext;
	private BasicHttpContext localcontext2;
	private String host = "localhost";
    private int port = 9000;

	private void start() throws IOException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		initializeHttp();
		File f = new File("/Users/dhiller2/AAROOT/agglist.txt");
		FileReader r = new FileReader(f);
		BufferedReader reader = new BufferedReader(r);

		Set<String> aggregations = new HashSet<String>();
		while(true) {
			String line = reader.readLine();
			if(line == null)
				break;

			String name = line;
			aggregations.add(name);
		}

		Set<String> tables = new HashSet<String>();
		System.out.println("size="+aggregations.size());
		for(String name : aggregations) {
			processAggregation(name, tables);
		}
		
		System.out.println("tables size="+tables.size());
		for(String table : tables) {
			System.out.println(table);
		}
	}

	private void initializeHttp() throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		SSLContext sslContext = initialize();
		org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext);
		PlainSocketFactory socketFactory = new PlainSocketFactory();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", 443, sslSocketFactory));
        schemeRegistry.register(new Scheme("http", 8080, socketFactory));

        ClientConnectionManager clientConnectionManager = 
                new ThreadSafeClientConnManager(schemeRegistry);
        
        httpclient = new DefaultHttpClient(clientConnectionManager);
        localcontext = setupPreEmptiveBasicAuth(httpclient, "dhiller2", "register:10768272983:b1:1501238151389733418", "databus.nrel.gov", 443);
		localcontext2 = setupPreEmptiveBasicAuth(httpclient, "dhiller2", "register:10768272983:b1:1501238151389733418", host, port);
	}

	private void processAggregation(String name, Set<String> tables) throws IllegalStateException, IOException {
		String url = "https://databus.nrel.gov/api/sumstreamsV1/aggregation/"+name;
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget, localcontext);
		HttpEntity entity = response.getEntity();

		InputStream instream = entity.getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(instream, writer);
		String theString = writer.toString();

		ObjectMapper mapper = new ObjectMapper();
		RegisterAggregation root = mapper.readValue(theString, RegisterAggregation.class);
		List<String> urls = root.getUrls();
		
		for(String aggUrl : urls) {
			String[] split = aggUrl.split("/");
			String last = split[split.length-1];
			String module = split[split.length-2];
			if("rawdataV1".equals(module)) {
				tables.add(last);
				return;
			}
			
			processAggregation(last, tables);
		}
	}
	
	public static BasicHttpContext setupPreEmptiveBasicAuth(DefaultHttpClient httpclient, String user, String apiKey, String hostName, int port) {
		HttpHost host = new HttpHost(hostName, port);
		httpclient.getCredentialsProvider().setCredentials(
		        new AuthScope(hostName, port), 
		        new UsernamePasswordCredentials(user, apiKey));
	
		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);
	
		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}
	
	private static SSLContext initialize() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
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
}
