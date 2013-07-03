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

import play.mvc.Controller;
import play.mvc.Http.Request;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.meta.DboColumnCommonMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnToOneMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

import controllers.api.ApiRegistrationImpl;

public class ApiRegistration extends Controller {

	/**
	 * ObjectMapper for marshalling JSON
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(ApiRegistration.class);

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
		renderJSON(jsonStr);
	}

}
