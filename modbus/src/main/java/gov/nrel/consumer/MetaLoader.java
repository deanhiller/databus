package gov.nrel.consumer;

import gov.nrel.consumer.beans.Device;
import gov.nrel.consumer.beans.Stream;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.codehaus.jackson.map.ObjectMapper;

public class MetaLoader {

	private static final Logger log = Logger.getLogger(MetaLoader.class.getName());
	private Set<Device> registeredDevices = new HashSet<Device>();
	private Set<Stream> registeredStreams = new HashSet<Stream>();
	private ObjectMapper mapper = new ObjectMapper();
	private ExecutorService recorderSvc;
	private String username;
	private String key;
	private String host;
	private int port;
	private String hostUrl;
	private String mode;
	
	public void initialize(String username, String key, String host, int port, boolean isSecure, DefaultHttpClient httpclient, String deviceTable, String streamTable, ExecutorService recorderSvc) {
		this.username = username;
		this.key = key;
		this.recorderSvc = recorderSvc;
		this.host = host;
		this.port = port;
		this.mode = "https";
		if(!isSecure)
			mode = "http";
		this.hostUrl = mode+"://"+host+":"+port;
		
		//need to read in all stuff here
		loadDevices(httpclient, deviceTable, streamTable);
	}

	private void loadDevices(DefaultHttpClient httpclient, String deviceTable, String streamTable) {
		String sql = "select+*+from+"+deviceTable;
		log.info("run sql to get all devices="+sql);
		long start = System.currentTimeMillis();
		List<Map<String, String>> rows = load("devices", httpclient, sql);
		
		CountDownLatch latch = new CountDownLatch(rows.size());
		for(Map<String, String> row : rows) {
			LoadDevice dev = new LoadDevice(httpclient, row, streamTable, latch);
			recorderSvc.execute(dev);
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		long total = System.currentTimeMillis()-start;
		log.info("DONE LOADING, time="+total+" ms");
	}

	private class LoadDevice implements Runnable {
		private DefaultHttpClient httpClient;
		private Map<String, String> row;
		private String streamTable;
		private CountDownLatch latch;

		public LoadDevice(DefaultHttpClient httpclient,
				Map<String, String> row, String streamTable, CountDownLatch latch) {
			this.httpClient = httpclient;
			this.row = row;
			this.streamTable = streamTable;
			this.latch = latch;
		}

		@Override
		public void run() {
			try {
				loadDevice(httpClient, row, streamTable);
			} catch(Exception e) {
				log.log(Level.WARNING, "Exception loading device", e);
			} finally {
				latch.countDown();
			}
		}
		
	}
	
	private void loadDevice(DefaultHttpClient httpclient, Map<String, String> row, String streamTable) {
		String id = row.get("id");
		Device d = new Device();
		d.setDeviceId(id);
		addDevice(d);

		String sql = "select+s+from+"+streamTable+"+as+s+where+s.device='"+id+"'";
		log.info(id+" run sql="+sql);
		List<Map<String, String>> streams = load(id, httpclient, sql);
		log.info(id+" loaded size="+streams.size());
		List<String> streamNames = new ArrayList<String>();
		for(Map<String, String> srow : streams) {
			String tableName = srow.get("stream");
			Stream s = new Stream();
			s.setTableName(tableName);
			streamNames.add(tableName);
			addStream(s);
		}
		
		log.info(id+" loaded device="+id+" streams="+streamNames);
	}

	public synchronized boolean addStream(Stream s) {
		return registeredStreams.add(s);
	}
	
	public synchronized boolean addDevice(Device d) {
		return registeredDevices.add(d);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Map<String, String>> load(String id, DefaultHttpClient httpclient, String sql) {
		String theString = "";
		try {
			BasicHttpContext ctx = setupPreEmptiveBasicAuth(httpclient);
			String getUrl = hostUrl+"/api/getdataV1/"+sql;
			log.info("getUrl for load="+getUrl);
			HttpGet get = new HttpGet(getUrl);
			long t1 = System.currentTimeMillis();
			HttpResponse resp = httpclient.execute(get, ctx);
			
			HttpEntity entity = resp.getEntity();
			InputStream instream = entity.getContent();
			StringWriter writer = new StringWriter();
			IOUtils.copy(instream, writer);
			theString = writer.toString();

			long t2 = System.currentTimeMillis();
			log.info(id+" time to get response="+(t2-t1)+" ms");
			
			if (resp.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("failure="+resp.getStatusLine()+" body="+theString);
			}
			
			Map root = mapper.readValue(theString, Map.class);
			List<Map<String, String>> object = (List<Map<String, String>>) root.get("data");
			return object;
		} catch(Exception e) {
			log.info("exception loading");
			throw new RuntimeException("error on processing.  string body returned="+theString, e);
		}
	}
	
	BasicHttpContext setupPreEmptiveBasicAuth(DefaultHttpClient httpclient) {
		HttpHost targetHost = new HttpHost(host, port, mode); 
		httpclient.getCredentialsProvider().setCredentials(
		        new AuthScope(targetHost.getHostName(), targetHost.getPort()), 
		        new UsernamePasswordCredentials(username, key));

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
}
