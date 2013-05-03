package controllers.modules;

import java.util.List;

import models.PermissionType;
import models.SecureSchema;
import models.StreamAggregation;
import models.message.RegisterAggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;

import com.alvazan.play.NoSql;

import controllers.Parsing;
import controllers.SecurityUtil;

public class StreamAggregateMod extends Controller {

	private static final Logger log = LoggerFactory.getLogger(StreamAggregateMod.class);

	public static void getAggregation(String name) {
		StreamAggregation s = NoSql.em().find(StreamAggregation.class, name);
		if(s == null)
			notFound("StreamAggregation="+name+" not found");
		else if(!SecurityUtil.hasRoleOrHigher(s.getSchema().getId(), PermissionType.READ))
			unauthorized("You have no access to group="+name);
		
		RegisterAggregation agg = new RegisterAggregation();
		agg.setDatabase(s.getSchema().getName());
		agg.setGroup(s.getSchema().getName());
		agg.setName(name);
		agg.setUrls(s.getUrls());
		
		String jsonString = Parsing.parseJson(agg);
		String param = request.params.get("callback");
		if(param != null) {
			response.contentType = "text/javascript";
			jsonString = param + "(" + jsonString + ");";
		}
		renderJSON(jsonString);
	}
	
	public static void postAggregation() {
		String json = Parsing.fetchJson();
		RegisterAggregation data = Parsing.parseJson(json, RegisterAggregation.class);
		
		String schemaName = data.getGroup();
		SecureSchema schema = SecureSchema.findByName(NoSql.em(), schemaName);
		if(schema == null) {
			if (log.isInfoEnabled())
				log.info("schema not found="+schemaName);
			notFound("Database does not exist="+schemaName);
		} else if(!SecurityUtil.hasRoleOrHigher(schema.getId(), PermissionType.READ_WRITE))
			unauthorized("You have no reader/writer access to schema="+schemaName);

		StreamAggregation s = NoSql.em().find(StreamAggregation.class, data.getName());
		if(s == null) {
			s = new StreamAggregation();
		} else {
			if(!s.getSchema().getName().equals(schemaName))
				unauthorized("Aggregation name="+data.getName()+" is already in use under group="+schemaName);
		}

		s.setName(data.getName());
		List<String> urls = s.getUrls();
		urls.clear();
		
		for(String url : data.getUrls()) {
			urls.add(url);
		}
		
		s.setSchema(schema);
		NoSql.em().put(s);
		NoSql.em().put(schema);
		NoSql.em().flush();
		ok();
	}
	
	public static void postDelete(String name) {
		StreamAggregation s = NoSql.em().find(StreamAggregation.class, name);
		if(s == null)
			notFound("StreamAggregation="+name+" not found");
		else if(!SecurityUtil.hasRoleOrHigher(s.getSchema().getId(), PermissionType.READ_WRITE))
			unauthorized("You have no reader/writer access to schema="+name);
		
		SecureSchema schema = s.getSchema();
		schema.getAggregations().remove(s);
		
		NoSql.em().put(schema);
		NoSql.em().remove(s);
		NoSql.em().flush();

		ok();
	}

}
