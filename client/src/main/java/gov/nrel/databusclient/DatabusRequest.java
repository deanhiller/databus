package gov.nrel.databusclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;

public class DatabusRequest {
	private static final Logger log = Logger.getLogger(DatabusSender.class
			.getName());
	String request;
	String url;
	String robot;
	String key;
	int port;
	String mode = "https";
	String host;
	DefaultHttpClient httpclient;

	public static void main(String[] args) {
		// example using DatabusRequest

		DatabusRequest req = new DatabusRequest("https://databus.nrel.gov",
				"robot-modbus", "941RCGA.B2.1JGZZ11OZOCEZ");

		System.out.println(req
				.invertV1()
				.splinesV1("limitderivative", 60000)
				.rangecleanV1(-20000000, 20000000)
				.get("D73937843phaseRealPower", "1360771000000",
						"1360771480000"));
		
		// sum streams example
		System.out.println(req.sumstreamsV1().get("RSF_PV", "1360771000000",
				"1360771480000"));
		
		req = new DatabusRequest("https://databus.nrel.gov/api/getdataV1/select+*+from+modbusdeviceMeta",
				"robot-modbus", "941RCGA.B2.1JGZZ11OZOCEZ");
		req.request();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		DatabusRequest clone = new DatabusRequest(this.url, this.robot,
				this.key);
		clone.request = this.request;
		return clone;
	}

	public DatabusRequest(String url, String robot, String key) {
		this.url = url;

		if (url.contains(":") && url.split(":").length > 2) {
			try {
				this.port = new Integer(url.split(":")[2]);
			} catch (NumberFormatException e) {
				log.info("Cannot format port \"" + url.split(":")[2]
						+ "\" using default port 443");
				this.port = 443;
			}
		}
		if (url.contains(":") && url.split(":").length > 1) {
			try {
				this.port = new Integer(url.split(":")[1]);
			} catch (NumberFormatException e) {
				log.info("Cannot format port \"" + url.split(":")[1]
						+ "\" using default port 443");
				this.port = 443;
			}
		} else {
			this.port = 443;
		}

		if (url.startsWith("https://")) {
			this.host = url.replace("https://", "");
		} else {
			this.host = url;
		}

		if (this.host.contains(":")) {
			this.host = this.host.split(":")[0];
		}

		this.robot = robot;
		this.key = key;
		this.request = url + "/api/";

		PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
		mgr.setDefaultMaxPerRoute(30);
		mgr.setMaxTotal(30);
		httpclient = createSecureOne(mgr);
	}

	public String get(String stream, String startTime, String endTime) {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "rawdataV1/" + stream + "/" + startTime + "/"
					+ endTime;
			return next.request();
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest invertV1() {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "invertV1/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest nullV1() {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "nullV1/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest splineV1(long interval, long windowSize) {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "splineV1/" + interval + "/" + windowSize + "/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest splinesV1(String type, long windowSize) {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "splinesV1/" + type + "/" + windowSize + "/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest rangecleanV1(long min, long max) {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "rangecleanV1/" + min + "/" + max + "/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public DatabusRequest stddevV1(long rowCount, double factor) {
		try {
			DatabusRequest next = (DatabusRequest) this.clone();
			next.request += "stddevV1/" + rowCount + "/" + factor + "/";
			return next;
		} catch (CloneNotSupportedException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public sumStreamsRequest sumstreamsV1() {
		return new sumStreamsRequest(this);
	}

	private class sumStreamsRequest {
		DatabusRequest parentRequest;

		sumStreamsRequest(DatabusRequest request) {
			this.parentRequest = request;
		}

		public String get(String aggregation, String startTime, String endTime) {
			try {
				DatabusRequest next = (DatabusRequest) this.parentRequest
						.clone();
				next.request += "sumstreamsV1/" + aggregation + "/" + startTime
						+ "/" + endTime;
				return next.request();
			} catch (CloneNotSupportedException e) {
				log.info(e.getMessage());
			}
			return null;
		}
	}

	public String request() {
		String theString = "";
		try {
			BasicHttpContext ctx = setupPreEmptiveBasicAuth(this.httpclient);
			log.info("getUrl for load=" + this.request);
			HttpGet get = new HttpGet(this.request);
			long t1 = System.currentTimeMillis();
			HttpResponse resp = httpclient.execute(get, ctx);

			HttpEntity entity = resp.getEntity();
			InputStream instream = entity.getContent();
			StringWriter writer = new StringWriter();
			IOUtils.copy(instream, writer);
			theString = writer.toString();

			long t2 = System.currentTimeMillis();
			log.info("for request=" + this.request + " time to get response="
					+ (t2 - t1) + " ms");

			if (resp.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("failure=" + resp.getStatusLine()
						+ " body=" + theString);
			}

			return theString;
		} catch (Exception e) {
			throw new RuntimeException(
					"error on processing.  string body returned=" + theString,
					e);
		}
	}

	private DefaultHttpClient createSecureOne(PoolingClientConnectionManager mgr) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				public void checkClientTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			X509HostnameVerifier verifier = new X509HostnameVerifier() {

				@Override
				public void verify(String string, X509Certificate xc)
						throws SSLException {
				}

				@Override
				public void verify(String string, String[] strings,
						String[] strings1) throws SSLException {
				}

				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void verify(String arg0, SSLSocket arg1)
						throws IOException {
					// TODO Auto-generated method stub

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(verifier);
			SchemeRegistry sr = mgr.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, this.port));
			return new DefaultHttpClient(mgr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private BasicHttpContext setupPreEmptiveBasicAuth(
			DefaultHttpClient httpclient) {
		HttpHost targetHost = new HttpHost(this.host, this.port, this.mode);
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				new UsernamePasswordCredentials(this.robot, this.key));

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
