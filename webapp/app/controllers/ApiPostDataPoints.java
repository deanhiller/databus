package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;

import models.DataTypeEnum;
import models.KeyToTableName;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.MDC;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import play.Play;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.Realm.AuthScheme;

import controllers.api.ApiPostDataPointsImpl;
import controllers.modules2.framework.ProductionModule;

public class ApiPostDataPoints extends Controller {
	public static final int BATCH_SIZE = 100;
	//private static final String PREFIX = "keyToTable-";
	static final Logger log = LoggerFactory.getLogger(ApiPostDataPoints.class);

	public static ExecutorService executor = Executors.newFixedThreadPool(20);
	private static AsyncHttpClient client = ProductionModule.createSingleton();

	private static int counter = 0;
	private static int dataPointCounter = 0;
	private static Long firstTime = null;
	
	//temporarily remove MDC for this one log so it does not get filtered
	private synchronized static void logNoMdc(int numPoints) {
		if(firstTime == null)
			firstTime = System.currentTimeMillis();
		
		counter++;
		dataPointCounter += numPoints;
		if(counter % 100 == 0) {
			Object old = MDC.get("filter");
			MDC.remove("filter");
			long timeSinceStart = (System.currentTimeMillis()-firstTime)/1000;
			if (log.isInfoEnabled())
				log.info("Processing request number="+counter+" numPointsPosted="+dataPointCounter+" in total time="+timeSinceStart+" seconds");
			MDC.put("filter", old);
		}
	}
	
	public static void postData() throws SolrServerException, IOException, ParserConfigurationException, SAXException {
		String mode = (String) Play.configuration.get("upgrade.mode");
		String requestUrl = null;
		if(mode != null && mode.startsWith("http")) {
			requestUrl = mode;
		}

		String json = Parsing.fetchJson();
		ListenableFuture<Response> future = null;
		try {
			Map<String, Object> data = Parsing.parseJson(json, Map.class);
			String user = request.user;
			String password = request.password;
			
			if(requestUrl != null) {
				// fix this so it is passed in instead....
				Realm realm = new Realm.RealmBuilder()
						.setPrincipal(user)
						.setPassword(password)
						.setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
						.build();

				String fullUrl = requestUrl+"/api/postdataV1";

				RequestBuilder b = new RequestBuilder("POST")
						.setUrl(fullUrl)
						.setRealm(realm)
						.setBody(json);
				com.ning.http.client.Request httpReq = b.build();

				try {
					future = client.executeRequest(httpReq);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			int numPoints = ApiPostDataPointsImpl.postDataImpl(json, data, user, password, Request.current().path);

			logNoMdc(numPoints);
		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("Exception on posting json="+json);
			throw new RuntimeException(e);
		}
		
		if(future != null) {
			try {
				Response response = future.get();
				if(response.getStatusCode() != 200) {
					RuntimeException e = new RuntimeException("status code="+response.getStatusCode());
					e.fillInStackTrace();
					log.warn("Exception on second request", e);
					throw e;
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
