package controllers.gui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import models.message.StreamEditor;
import models.message.StreamModule;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
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
		try {
			encoding = compress(encoding);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		variablesMap.put("url", "/api/streamV1(encoding="+encoding+")");
		String encoded = ChartUtil.encodeVariables(variablesMap );
		return encoded;
	}

	
	public static String compress(String srcTxt)
		      throws IOException {
		    ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
		    GZIPOutputStream zos = new GZIPOutputStream(rstBao);
		    zos.write(srcTxt.getBytes());
		    IOUtils.closeQuietly(zos);
		     
		    byte[] bytes = rstBao.toByteArray();
		    
		    return Base64.encodeBase64URLSafeString(bytes);
	}
	
	public static String uncompress(String zippedBase64Str)
		      throws IOException {
		    String result = null;
		     
		    byte[] bytes = Base64.decodeBase64(zippedBase64Str);
		    GZIPInputStream zi = null;
		    try {
		      zi = new GZIPInputStream(new ByteArrayInputStream(bytes));
		      result = IOUtils.toString(zi);
		    } finally {
		      IOUtils.closeQuietly(zi);
		    }
		    return result;
		  }


//	private static String compress(String str) throws IOException {
//	    if (str == null || str.length() == 0) {
//	        return str;
//	    }
//	    ByteArrayOutputStream out = new ByteArrayOutputStream();
//	    GZIPOutputStream gzip = new GZIPOutputStream(out);
//	    gzip.write(str.getBytes());
//	    gzip.close();
//	    return out.toString("US-ASCII");
//	 }
//	
//	private static String uncompress(String str) throws IOException {
//		
//		ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes("US-ASCII"));
//		GZIPInputStream gzis = new GZIPInputStream(bais);
//		InputStreamReader reader = new InputStreamReader(gzis);
//		BufferedReader in = new BufferedReader(reader);
//
//		String result;
//		result = in.readLine();
//		return result;
//		    
//	 }

	public static StreamEditor decode(Map<String, String> vars) {
		String url = vars.get("url");
		String match = "streamV1(encoding=";
		int indexOf = url.indexOf(match);
		if(indexOf < 0) {
			int lastIndex = url.lastIndexOf("/");
			String tableName = url.substring(lastIndex+1);
			StreamModule m = new StreamModule();
			m.setModule("rawdataV1");
			m.getParams().put("table", tableName);
			m.getParams().put("columnName", "value");
			StreamEditor editor = new StreamEditor();
			editor.getStream().getStreams().add(m);
			return editor;
		}
		String post = url.substring(indexOf+match.length());
		int index2 = post.indexOf(")");
		String encoding = post.substring(0, index2);
		
		try {
			return decodeSimple(encoding);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public static StreamEditor decodeSimple(String encoding) throws IOException{
		System.out.println("decoding the string, len is "+encoding.length()+", string is "+encoding);
		encoding = uncompress(encoding);
		System.out.println("after uncompressing, len is "+encoding.length()+", string is "+encoding);
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
