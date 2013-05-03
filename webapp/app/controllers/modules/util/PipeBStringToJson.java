package controllers.modules.util;

import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.HttpResponseStatus;


class PipeBStringToJson {
	
	private static final Logger log = LoggerFactory.getLogger(PipeBStringToJson.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private String cache="";
	private boolean strippedHeader = false;
	private PipeCPromiseWriter promiseWriter;
	private String url;
	private boolean firstChunk = true;
	
	public PipeBStringToJson(PipeCPromiseWriter promiseWriter, String url) {
		this.url = url;
		this.promiseWriter = promiseWriter;
	}
	public void onBodyPartReceived(String s) throws JsonParseException, JsonMappingException, IOException {
		try {
			onBodyPartReceivedImpl(s);
		} catch(Exception e) {
			RuntimeException ee = new RuntimeException("failed processing cache="+cache, e);
			ee.fillInStackTrace();
			onThrowable(url, ee);
		}
	}
	
	private void onBodyPartReceivedImpl(String s) throws JsonParseException, JsonMappingException, IOException {
		cache += s;
		//if (log.isInfoEnabled())
			//log.info("thecache="+cache);
		if(!strippedHeader) {
			int index = cache.indexOf("[");
			if(index >= 0) {
				cache = cache.substring(index+1);
				strippedHeader = true;
			}
		}
		
		if(!strippedHeader)
			return;
		
		List<TimeValue> rows = new ArrayList<TimeValue>();
		while(true) {
			//could change to regex as may be faster since it would loop once to find both { and } most likely..
			int braceInd = cache.indexOf("{");
			int endInd = cache.indexOf("}");
			
			if(braceInd < 0 || endInd < 0)
				break;

			String row = cache.substring(braceInd, endInd+1);
			cache = cache.substring(endInd+1);
			//if (log.isInfoEnabled())
				//log.info("thecache="+cache);
			TimeValue json = unmarshal(row);
			
			rows.add(json);
		}

		if(rows.size() <= 0)
			return;
		
		if(log.isDebugEnabled())
			log.debug("firing to promise 111 another chunk sent through firstrow time="+rows.get(0).getTime());
		JsonRow r = new JsonRow();
		r.setUrl(url);
		r.setJsonSnippet(rows);
		if(firstChunk) {
			r.setFirstRow(true);
			firstChunk = false;
		}
		
		promiseWriter.incomingJsonRow(r);
	}

	private TimeValue unmarshal(String row) {
		try {
			TimeValue json = mapper.readValue(row, TimeValue.class);
			return json;
		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("Exception parsing row="+row, e);
			throw new RuntimeException(e);
		}
	}
	
	public Object onCompleted(String url2) {
		if (log.isInfoEnabled())
			log.info("completed");
		promiseWriter.complete(url);
		return null;
	}
	public void onThrowable(String url2, Throwable ex) {
		promiseWriter.onThrowable(url2, ex);
	}
	public void onStatus(String url, HttpResponseStatus status) {
		promiseWriter.onStatus(url, status);
	}
}