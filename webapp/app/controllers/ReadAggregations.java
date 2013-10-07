package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.Counter;

public class ReadAggregations {

	private static final Logger log = LoggerFactory.getLogger(ReadAggregations.class);
	
	private Timer timer = new Timer();

	private ThreadSafeClientConnManager clientConnectionManager;

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
		File f = new File("/Users/dhiller2/AAROOT/rawtablelist.txt");
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

		log.info("size="+aggregations.size());
		long start = System.currentTimeMillis();
		Counter rowCounter = new Counter();
		Counter badRows = new Counter();
		Counter extraRows = new Counter();
		Counter missingRows = new Counter();
		int counter = 0;
		for(String name : aggregations) {
			processAggregation(name, rowCounter, badRows, extraRows, missingRows);
			counter++;
			long end = System.currentTimeMillis();
			long total = end-start;
			log.info("name="+name+" done.  numComplete="+counter+" bad rows="+badRows+" total time so far="+total+" extra="+extraRows+" missing="+missingRows);
		}
	}

	private void processAggregation(String name, Counter rowCounter, Counter badRows, Counter extraRows, Counter missingRows) throws ClientProtocolException, IOException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		log.info("starting to process agg="+name);
		
		SSLContext sslContext = initialize();
		org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext);
		PlainSocketFactory socketFactory = new PlainSocketFactory();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", 443, sslSocketFactory));
        schemeRegistry.register(new Scheme("http", 8080, socketFactory));

        clientConnectionManager = 
                new ThreadSafeClientConnManager(schemeRegistry);
        String host = "localhost";
        int port = 9000;
        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnectionManager);
        long timeout = 60; //60 seconds
        int timeoutInt = (int) (timeout*1000);
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), timeoutInt);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), timeoutInt);
        httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(timeout * 1000));
        httpClient.getParams().setParameter("http.protocol.head-body-timeout", timeout * 1000);
		BasicHttpContext localcontext = setupPreEmptiveBasicAuth(httpClient, "dhiller2", "register:10768272983:b1:1501238151389733418", "databus.nrel.gov", 443);
		BasicHttpContext localcontext2 = setupPreEmptiveBasicAuth(httpClient, "dhiller2", "register:10768272983:b1:1501238151389733418", host, port);

		int numDaysInterval = 10;
		long endLong = System.currentTimeMillis();
		DateTime end = new DateTime(endLong);
		end = end.minusDays(2);
		DateTime start = end.minusDays(numDaysInterval);
		DateTime finallyDone = start.minusYears(1);
		int rowCountTable = 0;
		long veryLastTimePt = end.getMillis();
		long intervalMillis = TimeUnit.MILLISECONDS.convert(5, TimeUnit.DAYS);
		while(start.isAfter(finallyDone)) {
			long startMillis = start.getMillis();
			long endMillis = end.getMillis();
			
			String url = "https://databus.nrel.gov/api/csv/rawdataV1/"+name+"/"+startMillis+"/"+endMillis;
			String url2 = "http://"+host+":"+port+"/api/csv/rawdataV1/"+name+"/"+startMillis+"/"+endMillis;

			//MyTimerTask task1 = new MyTimerTask(clientConnectionManager);
			//timer.schedule(task1, 60000); //allow 60 seconds

			clientConnectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
			clientConnectionManager.closeExpiredConnections();

			HttpGet httpget = new HttpGet(url);
			HttpResponse response1 = httpClient.execute(httpget, localcontext);
			HttpGet httpget2 = new HttpGet(url2);
			HttpResponse response2 = httpClient.execute(httpget2, localcontext2);
			BufferedReader read1 = toReader(response1);
			BufferedReader read2 = toReader(response2);

//			if(task1.wasRun()) {
//				log.warn("we have successfully started to continue again somehow...response2="+response2);
//			}
//			
//			task1.done();
			
			String line1 = read1.readLine();
			String line2 = read2.readLine();
			long timeLine1 = Long.MIN_VALUE;
			long timeLine2 = Long.MIN_VALUE;

			//first line is header or null
			if(line1 == null && line2 == null) {
				continue;
			} else if(line1 != null && line2 != null) {
			} else
				log.info("first line of queries are not matching headers....url1="+url+" url2="+url2);
			
			//we are looking for missing rows in the new data only
			long rowInCsv1 = 0;
			long rowInCsv2 = 0;
			while(true) {
				if(timeLine1 == timeLine2) {
					line1 = readNonNullNonMaxInt(read1, url, rowInCsv1);
					line2 = readNonNullNonMaxInt(read2, url2, rowInCsv2);
					rowInCsv1++;
					rowInCsv2++;
				} else if(timeLine1 > timeLine2) {
					missingRows.increment();
					line2 = readNonNullNonMaxInt(read2, url2, rowInCsv2);
					rowInCsv2++;
				} else {
					extraRows.increment();
					line1 = readNonNullNonMaxInt(read1, url, rowInCsv1);
					rowInCsv1++;
				}

				if(line1 == null && line2 == null)
					break;

				try {
					if(line1 == null)
						timeLine1 = Long.MAX_VALUE;
					else
						timeLine1 = parseTime(line1);
					if(line2 == null)
						timeLine2 = Long.MAX_VALUE;
					else
						timeLine2 = parseTime(line2);
					
					if(timeLine1 != timeLine2) {
						String msg = fetchString(badRows, extraRows, missingRows,
								url, url2, line1, line2, rowInCsv1, rowInCsv2);
						log.info("row missing:, "+msg);
						continue;
					} else if(!line1.equals(line2)) {
						badRows.increment();
						String msg = fetchString(badRows, extraRows, missingRows,
								url, url2, line1, line2, rowInCsv1, rowInCsv2);
						log.info("rows not matching, "+msg);
					}
					rowCounter.increment();
					rowCountTable++;
					if(rowCounter.getCount() % 1000 == 0) {
						String msg = fetchString(badRows, extraRows, missingRows,
								url, url2, line1, line2, rowInCsv1, rowInCsv2);
						log.info("(row based)num rows done="+rowCounter+" table '"+name+"' row count="+rowCountTable+" timestart="+start+" finally done at="+finallyDone+" "+msg);
					}
				} catch(Exception e) {
					String msg = fetchString(badRows, extraRows, missingRows,
							url, url2, line1, line2, rowInCsv1, rowInCsv2);
					throw new RuntimeException(msg, e);
				}
			}

			long distance = veryLastTimePt - start.getMillis();
			if(distance % intervalMillis == 0) {
				String msg = fetchString(badRows, extraRows, missingRows,
						url, url2, line1, line2, rowInCsv1, rowInCsv2);
				log.info("(time based)num rows done="+rowCounter+" table '"+name+"' row count="+rowCountTable+" timestart="+start+" finally done at="+finallyDone+" "+msg);
			}
			end = end.minusDays(numDaysInterval);
			start = start.minusDays(numDaysInterval);
		}
	}

	private String readNonNullNonMaxInt(BufferedReader read2, String url, long rowInCsv1) throws IOException {
		while(true) {
			String line = read2.readLine();
			if(line == null)
				return line;
			String[] split = line.split(",");
			if(split.length < 2) {
				log.info("WARNING: line NOT complete:"+line+" url="+url+" row="+rowInCsv1);
				return null;
			}
			if(!"null".equals(split[1])) {
				try {
					BigDecimal dec = new BigDecimal(split[1]);
					BigDecimal maxInt = new BigDecimal(Integer.MAX_VALUE);
					if(dec.compareTo(maxInt) != 0)
						return line;
				} catch(Exception e) {
					throw new RuntimeException("could not turn into BigDecimal. line="+line+" split="+split[1], e);
				}
			}
		}
	}

	private String fetchString(Counter badRows, Counter extraRows,
			Counter missingRows, String url, String url2, String line1,
			String line2, long rowInCsv1, long rowInCsv2) {
		String msg = "line1="+line1+" line2="+line2+" r1="+rowInCsv1+" r2="+rowInCsv2+" url1="+url+" url2="+url2+" bad="+badRows+" extra="+extraRows+" missing="+missingRows;
		return msg;
	}

	private long parseTime(String line1) {
		String[] split = line1.split(",");
		long time = Long.parseLong(split[0]);
		return time;
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
	
//	private static class MyTimerTask extends TimerTask {
//
//		private boolean done;
//		private boolean wasRun;
//		private ThreadSafeClientConnManager connMgr;
//
//		public MyTimerTask(ThreadSafeClientConnManager clientConnectionManager) {
//			this.connMgr = clientConnectionManager;
//		}
//
//		@Override
//		public void run() {
//			try {
//				synchronized(this) {
//					if(done)
//						return;
//					log.warn("Running timer to cancel threads");
//					connMgr.
//				}
//			} catch(Exception e) {
//				log.warn("Exception running timer task", e);
//			}
//			wasRun = true;
//		}
//
//		public boolean wasRun() {
//			return wasRun;
//		}
//
//		public void done() {
//			this.cancel();
//			this.done = true;
//		}
//	}
}
