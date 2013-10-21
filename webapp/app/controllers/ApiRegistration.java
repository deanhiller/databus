package controllers;

import gov.nrel.util.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import models.KeyToTableName;
import models.PermissionType;
import models.RoleMapping;
import models.SdiColumn;
import models.SdiColumnProperty;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;
import models.message.DatasetColumnModel;
import models.message.DatasetColumnSemanticModel;
import models.message.DatasetType;
import models.message.GroupKey;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http.Request;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.meta.DboColumnCommonMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnToOneMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

import controllers.api.ApiRegistrationImpl;
import controllers.modules2.framework.ProductionModule;
import controllers.modules2.framework.chain.AHttpChunkingListener;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.RemoteProcessor.AuthInfo;
import controllers.modules2.framework.translate.TranslationFactory;

public class ApiRegistration extends Controller {

	/**
	 * ObjectMapper for marshalling JSON
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(ApiRegistration.class);

	private static AsyncHttpClient client = ProductionModule.createSingleton();
	
	public static void postRegisterTableNew() {
		postRegisterTableImpl(null);
	}
	
	public static void postRegisterTable(String registerKey) {
		try {
			Object object = MDC.get("user");
			if(object == null)
				MDC.put("user", registerKey);
			postRegisterTableImpl(registerKey);
		} finally {
			MDC.remove("user");
		}
	}
	private static void postRegisterTableImpl(String registerKey) {
		String json = Parsing.fetchJson();
		RegisterMessage msg = Parsing.parseJson(json, RegisterMessage.class);
		Request request = Request.current();
		String username = request.user;
		String password = request.password;
		
		DatasetType type = msg.getDatasetType();
		//STREAM is no longer existing!!!
		if(type == DatasetType.STREAM)
			msg.setDatasetType(DatasetType.TIME_SERIES);

//		ListenableFuture<Response> future = null;
//		if(requestUrl != null) {
//			// fix this so it is passed in instead....
//			Realm realm = new Realm.RealmBuilder()
//					.setPrincipal(username)
//					.setPassword(password)
//					.setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
//					.build();
//
//			String fullUrl = requestUrl+"/api/registerV1";
//
//			RequestBuilder b = new RequestBuilder("POST")
//					.setUrl(fullUrl)
//					.setRealm(realm)
//					.setBody(json);
//			com.ning.http.client.Request httpReq = b.build();
//
//			int maxRetry = client.getConfig().getMaxRequestRetry();
//			try {
//				future = client.executeRequest(httpReq);
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}

		RegisterResponseMessage rmsg = ApiRegistrationImpl.registerImpl(msg, username, password);
		
		JsonNode jsonNode = mapper.valueToTree(rmsg);
		StringWriter strWriter = new StringWriter();

		try {
			mapper.writeValue(strWriter, jsonNode);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String jsonStr = strWriter.toString();
		if (log.isInfoEnabled())
			log.info("register successful for table="+msg.getModelName()+" response="+jsonStr);
		
//		if(future != null) {
//			try {
//				Response response = future.get();
//				if(response.getStatusCode() != 200) {
//					RuntimeException e = new RuntimeException("status code="+response.getStatusCode());
//					e.fillInStackTrace();
//					log.warn("Exception on second request", e);
//					throw e;
//				}
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			} catch (ExecutionException e) {
//				throw new RuntimeException(e);
//			}
//		}
		
		renderJSON(jsonStr);
	}

}
