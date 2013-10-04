package controllers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.Counter;

public class ReadAggregations {

	private static final Logger log = LoggerFactory.getLogger(ReadAggregations.class);
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
		new ReadAggregations().start();
	}

	private void start() throws IOException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
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

		System.out.println("size="+aggregations.size());
		long start = System.currentTimeMillis();
		Counter rowCounter = new Counter();
		int counter = 0;
		for(String name : aggregations) {
			processAggregation(name, rowCounter);
			counter++;
			long end = System.currentTimeMillis();
			long total = end-start;
			System.out.println("name="+name+" done.  numComplete="+counter+" total time so far="+total);
		}
	}

	private void processAggregation(String name, Counter rowCounter) throws ClientProtocolException, IOException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		System.out.println("starting to process agg="+name);
		
		SSLContext sslContext = initialize();
		org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext);
		PlainSocketFactory socketFactory = new PlainSocketFactory();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", 443, sslSocketFactory));
        schemeRegistry.register(new Scheme("http", 8080, socketFactory));

        ClientConnectionManager clientConnectionManager = 
                new ThreadSafeClientConnManager(schemeRegistry);
        
        String host = "localhost";
        int port = 9000;
        DefaultHttpClient httpclient = new DefaultHttpClient(clientConnectionManager);
		BasicHttpContext localcontext = setupPreEmptiveBasicAuth(httpclient, "dhiller2", "register:10768272983:b1:1501238151389733418", "databus.nrel.gov", 443);
		BasicHttpContext localcontext2 = setupPreEmptiveBasicAuth(httpclient, "dhiller2", "register:10768272983:b1:1501238151389733418", host, port);

		long endLong = System.currentTimeMillis();
		DateTime end = new DateTime(endLong);
		DateTime start = end.minusDays(1);
		DateTime finallyDone = start.minusYears(1);
		int rowCountTable = 0;
		long veryLastTimePt = end.getMillis();
		long intervalMillis = TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
		while(start.isAfter(finallyDone)) {
			long startMillis = start.getMillis();
			long endMillis = end.getMillis();
			
			String url = "https://databus.nrel.gov/api/csv/aggregation/"+name+"/"+startMillis+"/"+endMillis;
			String url2 = "http://"+host+":"+port+"/api/csv/aggregation/"+name+"/"+startMillis+"/"+endMillis;

			HttpGet httpget = new HttpGet(url);
			HttpResponse response1 = httpclient.execute(httpget, localcontext);
			HttpGet httpget2 = new HttpGet(url2);
			HttpResponse response2 = httpclient.execute(httpget2, localcontext2);
			BufferedReader read1 = toReader(response1);
			BufferedReader read2 = toReader(response2);

			long rowInCsv = 0;
			while(true) {
				String line1 = read1.readLine();
				String line2 = read2.readLine();
				rowInCsv++;
				if((line1 == null && line2 != null) || (line1 != null && line2 == null))
					throw new RuntimeException("bad comparison, line1="+line1+" line2="+line2+" query="+url+" query2="+url2+" at row="+rowCountTable+" time="+start+" rowCsv="+rowInCsv);
				else if(line1 == null && line2 == null)
					break;
				else if(!line1.equals(line2))
					throw new RuntimeException("DIFF. bad comparison, line1="+line1+" line2="+line2+" query="+url+" query2="+url2+" at row="+rowCountTable+" time="+start+" rowInCsv="+rowInCsv);
				rowCounter.increment();
				rowCountTable++;
				if(rowCounter.getCount() % 1000 == 0) {
					System.out.println("(row based)num rows done="+rowCounter+" table '"+name+"' row count="+rowCountTable+" timestart="+start+" finally done at="+finallyDone+" rowInCsv="+rowInCsv);
				}
			}

			long distance = veryLastTimePt - start.getMillis();
			if(distance % intervalMillis == 0) {
				System.out.println("(time based)num rows done="+rowCounter+" table '"+name+"' row count="+rowCountTable+" timestart="+start+" finally done at="+finallyDone);
			}
			end = end.minusDays(1);
			start = start.minusDays(1);
		}
	}

	private BufferedReader toReader(HttpResponse response1) throws IOException {
		HttpEntity entity = response1.getEntity();
		InputStream instream1 = entity.getContent();
		InputStreamReader r1 = new InputStreamReader(instream1);
		BufferedReader read1 = new BufferedReader(r1);
		return read1;
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
