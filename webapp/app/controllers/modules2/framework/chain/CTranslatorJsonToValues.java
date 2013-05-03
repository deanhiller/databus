package controllers.modules2.framework.chain;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.inject.Inject;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;

public class CTranslatorJsonToValues extends BFailureProcessor {

	private static final Logger log = LoggerFactory.getLogger(CTranslatorJsonToValues.class);

	@Inject
	private ObjectMapper mapper;
	private String cache="";
	private boolean strippedHeader = false;
	private MapType mapType;
	
	@Override
	protected void onBytesReceived(String url, byte[] data) {
		String s = new String(data, Charset.forName("ISO-8859-1"));
		onBodyPartReceived(url, s);
	}

	public void onBodyPartReceived(String url, String s) {
		try {
			onBodyPartReceivedImpl(url, s);
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
			
	}

	private void onBodyPartReceivedImpl(String url, String s) throws JsonParseException, JsonMappingException, IOException {
		cache += s;
		if(!strippedHeader) {
			int index = cache.indexOf("[");
			if(index >= 0) {
				cache = cache.substring(index+1);
				strippedHeader = true;
			}
		}
		
		if(!strippedHeader)
			return;
		int count = 0;
		while(true) {
			//could change to regex as may be faster since it would loop once to find both { and } most likely..
			int braceInd = cache.indexOf("{");
			int endInd = cache.indexOf("}");
			
			if(braceInd < 0 || endInd < 0)
				break;

			String row = cache.substring(braceInd, endInd+1);
			cache = cache.substring(endInd+1);
			TSRelational json = unmarshal(row);
			
			if(log.isDebugEnabled() && count == 0)
				log.debug("firing to chunk downstream firstrow of chunk time="+json.getTime());
			processRow(url, json);
			count++;
		}
	}

	private void processRow(String url, TSRelational json) {
		try {
			TcpFlowControlFlag flag = new TcpFlowControlFlag();
			processor.incomingChunk(url, json, flag);
		} catch(SuccessfulAbort e) {
			if(log.isDebugEnabled())
				log.debug("aborting processing of row since value or time is null..can be normal if downstream nulls out values");
			throw e;
		}
	}

	private TSRelational unmarshal(String row) {
		try {
			if(mapType == null) {
				TypeFactory factory = mapper.getTypeFactory();
				mapType = factory.constructMapType(TSRelational.class, String.class, Object.class);
			}
			
			TSRelational result = mapper.readValue(row, mapType);
			return result;
		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("Exception parsing row="+row, e);
			throw new RuntimeException(e);
		}
	}
}
