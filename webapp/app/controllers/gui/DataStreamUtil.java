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

	public static String encode(StreamEditor agg) {
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
		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put("url", "/api/streamV1(encoding="+encoding+")");
		String encoded = ChartUtil.encodeVariables(variablesMap );
		return encoded;
	}

	public static StreamEditor decode(String encoded) {
		if("start".equals(encoded))
			return new StreamEditor();
		
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
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
