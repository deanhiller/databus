package controllers.gui;

import java.io.IOException;

import models.message.StreamEditor;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

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
		String encoded = Base64.encodeBase64URLSafeString(bytes);
		return encoded;
	}

	public static StreamEditor decode(String encoded) {
		if("start".equals(encoded))
			return new StreamEditor();
		byte[] decoded = Base64.decodeBase64(encoded);
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
