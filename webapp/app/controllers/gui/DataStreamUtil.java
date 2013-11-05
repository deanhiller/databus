package controllers.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.message.StreamEditor;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import controllers.gui.util.ChartUtil;

public class DataStreamUtil {

	private static final ObjectMapper mapper = new ObjectMapper();

	public static String encode(StreamEditor agg, Map<String, String> variablesMap) {
		String first;
		try {
			first = mapper.writeValueAsString(agg);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		byte[] bytes = first.getBytes();
		String encoding = Base64.encodeBase64URLSafeString(bytes);
		variablesMap.put("url", "/api/streamV1(encoding="+encoding+")");
		String encoded = ChartUtil.encodeVariables(variablesMap );
		return encoded;
	}

	public static StreamEditor decode(Map<String, String> vars) {
		String url = vars.get("url");
		String match = "streamV1(encoding=";
		int indexOf = url.indexOf(match);
		String post = url.substring(indexOf+match.length());
		int index2 = post.indexOf(")");
		String encoding = post.substring(0, index2);
		
		return decodeSimple(encoding);
	}

	public static StreamEditor decodeSimple(String encoding) {
		byte[] decoded = Base64.decodeBase64(encoding);
		String json = new String(decoded);
		StreamEditor streamInfo;
		try {
			streamInfo = mapper.readValue(json, StreamEditor.class);
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return streamInfo;
	}

}
