package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

public class Parsing {

	private static final Logger log = LoggerFactory.getLogger(Parsing.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static String fetchJson() {
		try {
			Request request = Request.current();
			InputStream in = request.body;
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			String json = writer.toString();
			return json;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T parseJson(String json, Class<T> type) {
		try {
			if (log.isInfoEnabled())
				log.info("posting json="+json);
			return mapper.readValue(json, type);
		} catch (JsonParseException e) {
			if (log.isWarnEnabled())
        		log.warn("There was a problem parsing the JSON request. json="+json, e);
			throw new BadRequest("There was a problem parsing the JSON request. json="+json);
		} catch (JsonMappingException e) {
			if (log.isWarnEnabled())
        		log.warn("Your json request appears to be invalid. json="+json, e);
			throw new BadRequest("Your json request appears to be invalid.  json="+json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String parseJson(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
