package controllers.modules;

import gov.nrel.util.SearchUtils;
import gov.nrel.util.TimeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import models.PermissionType;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.StreamAggregation;
import models.message.RegisterAggregation;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import play.mvc.Controller;
import play.mvc.Http.Response;

import com.alvazan.play.NoSql;

import controllers.Parsing;
import controllers.SecurityUtil;
import controllers.modules.util.Info;
import controllers.modules.util.JsonRow;
import controllers.modules.util.ModuleStreamer;
import controllers.modules.util.OurPromise;
import controllers.modules.util.Processor;

public class SumStreamsMod extends Controller {

	public static final Logger log = LoggerFactory.getLogger(SumStreamsMod.class);
	
	/*
	 * Public Methods (for Play they're implicit Controllers)
	 */	
	public static void error(String path) {
		badRequest("Your url is invalid as you need sumstreamsV1/{name}/{start}/{end}... and start and end have to be longs");
	}
	
	public static void storedAggregation(String name, long start, long end) throws InterruptedException, ExecutionException, IOException {
		
		StreamAggregation agg = NoSql.em().find(StreamAggregation.class, name);
		if(agg == null)
			notFound("Aggregation named="+name+" was not found");
		List<String> urls = agg.getUrls();
		if(urls.size() == 0)
			notFound("Aggregation named="+name+" has not paths that we can read from");
		
		String param = request.params.get("callback");
		
		String postUrl = "/"+start+"/"+end;
		Processor p;
		if(request.isNew) { 
			p = createProcessor(urls, start, end, param);
			request.args.put("processor", p);
		} else {
			p = (Processor) request.args.get("processor");
		}
		
		Info info = ModuleStreamer.startStream(urls, postUrl);
		CountDownLatch latch = info.getLatch();
		OurPromise<Object> promise = info.getPromise();
		
		while(latch.getCount() != 0) {
			await(promise);
			ModuleStreamer.processResponses(info, p);
		}

		Long startTime = (Long) request.args.get("time");
		long total = System.currentTimeMillis() - startTime;
		if (log.isInfoEnabled())
			log.info("finished with request to urls="+urls+" total time="+total+" ms");
	}
	
	/*
	 * Aggregation logic moved from StreamAggregateMod.java per Dean
	 */
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
	
	public static void getAllAggregations() {
		/*
		 * Stubbed out in case we ever need it.  Backend currently doesnt support it (not a huge
		 * change) but we dont need this yet.
		 */
		/*
		Request request = Request.current();	
		
		EntityUser user = NoSql.em().find(EntityUser.class, request.user);
		String result = SumStreamsMod.getAggregations(user);
		*/
		
		/*
         * {
			    "aggregations": [
			        {
			            "database": "<database1>",
			            "group": "<database1>",
			            "aggregation": [
			                "<aggregation>",
			                "<aggregation>"
			            ]
			        },
			        {
			            "database": "<database2>",
			            "group": "<database2>",
			            "aggregation": [
			                "<aggregation>",
			                "<aggregation>"
			            ]
			        }
			    ]
			}
         */
		
		Map<String, List<String>> result = SumStreamsMod.getAggregations();  //<dbID, aggregationIDs>

		StringBuffer jsonString = new StringBuffer();
		 jsonString.append("{\"aggregations\":[{");
		Iterator<Entry<String, List<String>>> keyItr = result.entrySet().iterator();
	    while (keyItr.hasNext()) {
	        Map.Entry<String, List<String>> pairs = (Map.Entry)keyItr.next();
	        
	        String db = pairs.getKey();
	        List<String> aggs = pairs.getValue();
	       
	        jsonString.append("\"database\":\"" + db + "\",");
	        jsonString.append("\"group\":\"" + db + "\",");
	        jsonString.append("\"aggregation\":[");
	        
	        Iterator<String> aggItr = aggs.iterator();
	        while(aggItr.hasNext()) {
	        		String agg = aggItr.next();
	        		jsonString.append("\"" + agg + "\"");
	        		
	        		if(aggItr.hasNext()) {
	        			jsonString.append(",");
	        		}
	        }
	        jsonString.append("]}");
	        
	        if(keyItr.hasNext()) {
	        		jsonString.append(",{");
	        }
	    }
	    jsonString.append("]}");
	    
	    renderJSON(jsonString.toString());
	}

	/*
	 * Stubbed out in case we ever need it.  Backend currently doesnt support it (not a huge
	 * change) but we dont need this yet.
	 */
	/*
	public static void getAllAggregationsByUser(String name) {		
		EntityUser user = null;
		if((null == name) || (name.equals(""))) {
			Request request = Request.current();			
			user = NoSql.em().find(EntityUser.class, request.user);
		} else {
			user = NoSql.em().find(EntityUser.class, name);
		}
		
		if(null == user) {	
			// Send invalid user response
			String result = "(Invalid User)";
			String param = request.params.get("callback");
			if(param != null) {
				response.contentType = "text/javascript";
				result = param + "(Invalid user);";
			}
			renderJSON(result);
			return;
		}
		
		if (log.isInfoEnabled())
			log.info("\n\n\n\n\nUser Lookup Finished!  User is: " +user.getName());
		String result = SumStreamsMod.getAggregations(user);
	}
	*/
	
	public static void postAggregation() {
		String json = Parsing.fetchJson();
		RegisterAggregation data = Parsing.parseJson(json, RegisterAggregation.class);
		
		String schemaName = data.getDatabase();
		if(schemaName == null)
			schemaName = data.getGroup();
		
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
		
		try {
			SearchUtils.indexAggregation(s, schema);
		} catch (Exception e) {
			log.error("indexing aggregation "+s.getName()+" failed!  Run reindex later!");
			log.error("exception is "+e.getMessage());
			e.printStackTrace();
		} 
		ok();
	}
	
	public static void postDelete(String name) {
		StreamAggregation s = NoSql.em().find(StreamAggregation.class, name);
		if(s == null)
			notFound("StreamAggregation="+name+" not found");
		else if(!SecurityUtil.hasRoleOrHigher(s.getSchema().getId(), PermissionType.READ_WRITE))
			unauthorized("You have no reader/writer access to schema="+name);
		
		NoSql.em().remove(s);
		NoSql.em().flush();

		ok();
	}
	
	/*
	 * Private Methods
	 */	
	private static Processor createProcessor(List<String> urls, long start, long end, String param) {
		Processor p;
		if(urls.size() == 1) {
			//this prevents usage of the spline curve for single item data so we do not modify the
			//data at all.
			p = new PassthroughProcessor(param);
		} else
			p = new ProcessorImpl(urls, start, end, param);
		return p;
	}
	
	private static Map<String, List<String>> getAggregations() {
		// First, get all of the databases this user has access to
		Map<String, SecureResourceGroupXref> dbs = SecurityUtil.groupSecurityCheck();
		
		// Second, for each Database, get all of the aggregations
		// The SecureResourceGroupXref HAS the DB embedded in it which means each embedded
		// db has all of its aggregations.  We dont need to hit the DB anymore
		Map<String, List<String>> aggregationsByDb = new TreeMap<String, List<String>>(); // <dbID, aggregationIDs>
		for (SecureResourceGroupXref xref : dbs.values()) {
			SecureSchema db = (SecureSchema)xref.getResource();
			List<StreamAggregation> aggregations = db.getAggregations();
						
			List<String> aggNames = new ArrayList<String>();
			for(StreamAggregation agg: aggregations) {
				aggNames.add(agg.getName());
			}
			
			aggregationsByDb.put(db.getName(), aggNames);			
		}
		
		return aggregationsByDb;		
	}
	
	public static class ProcessorImpl implements Processor {

		private Map<String, SumCache> caches = new HashMap<String, SumCache>();
		private boolean initialized;
		private int numLeft;
		private boolean isFirstRow = true;
		private String param;
		private Throwable cause=null;
		
		public ProcessorImpl(List<String> urls, long start, long end, String param) {
			for(String url : urls) {
				caches.put(url, new SumCache(url));
			}
			numLeft = urls.size();
			this.param = param;
		}
		
		@Override
		public void onStart() {
			ModuleStreamer.writeHeader(param);
		}

		@Override
		public void complete(String url) {
			SumCache aggCache = caches.get(url);
			aggCache.setComplete(true);
			numLeft--;
			if (log.isInfoEnabled())
				log.info("numleft on urls="+numLeft+" url complete="+url);
			if(numLeft == 0) {
				processLeftOvers();
				ModuleStreamer.writeFooter(param, getCause());
				
				String urlsAndSizes = "";
				for(SumCache c : caches.values()) {
					urlsAndSizes += "url="+c.getUrl()+" totalCnt="+c.getTotalCount()+"\n";
				}
				if (log.isInfoEnabled())
					log.info("urls and sizes=\n"+urlsAndSizes);
			}
		}

		private void processLeftOvers() {
			int maxSize = 0;
			//Now, completed streams return MAX_VALUE, but for the ones
			//that are not complete, we need the min row count so we only 
			//process min rows until that stream catches up.
			for(SumCache c : caches.values()) {
				int count = c.getRowCount();
				maxSize = Math.max(maxSize, count);
			}
			
			processAlignedRows(maxSize);
		}
		
		@Override
		public void incomingJsonRow(boolean isFirstChunk, JsonRow chunk) {
			SumCache aggCache = caches.get(chunk.getUrl());
			aggCache.addChunk(chunk);
			if(initialized) {
				processWhatWeCan(chunk);
			} else if(allCachesReadyToStart()) {
				initialized = true;
				processWhatWeCan(chunk);
			}
		}
		
		private void processWhatWeCan(JsonRow chunk) {
			int minSize = Integer.MAX_VALUE;
			//Now, completed streams return MAX_VALUE, but for the ones
			//that are not complete, we need the min row count so we only 
			//process min rows until that stream catches up.
			for(SumCache c : caches.values()) {
				int count = c.getRowCount();
				minSize = Math.min(minSize, count);
			}
			
			processAlignedRows(minSize);
		}

		private void processAlignedRows(int numRows) {
			List<TimeValue> values = new ArrayList<TimeValue>();
			for(int i = 0; i < numRows; i++) {
				Long time = null;
				BigDecimal val = new BigDecimal("0.0");
				List<Long> times = new ArrayList<Long>();
				List<String> urls = new ArrayList<String>();
				boolean failed = false;
				for(SumCache c : caches.values()) {
					TimeValue tv = c.removeNextRow();
					if(tv == null)
						continue;
					
					if(time == null)
						time = tv.getTime();
					else if(time.longValue() != tv.getTime())
						failed = true;
					
					times.add(tv.getTime());
					urls.add(c.getUrl());
					val = val.add(tv.getValue()==null?new BigDecimal("0.0"):tv.getValue());
				}

				if(failed) {
					if (log.isWarnEnabled())
		        		log.warn("failure in their code or ours?  stream times="+times+"\nurls="+urls);
					throw new RuntimeException("Your streams are not aligned, run spline modules before aggregation!!!!");
				}
				
				values.add(new TimeValue(time, val));
			}
			
			writeChunk(values);
		}

		private boolean allCachesReadyToStart() {
			for(SumCache c: caches.values()) {
				if(!c.hasData())
					return false;
			}
			
			return true;
		}

		private void writeChunk(List<TimeValue> rows) {
			Response response = Response.current();
			
			String resultingJson = "";
			for(int i = 0; i < rows.size(); i++) {
				TimeValue r = rows.get(i);
				
				if(isFirstRow) {
					isFirstRow = false;
				} else {
					resultingJson += ",";
				}
				resultingJson = ModuleStreamer.translateToString(resultingJson, r);
			}

			if(resultingJson.length() > 0) 
				response.writeChunk(resultingJson);
		}

		@Override
		public void onFailure(String url, Throwable exception) {
			if (log.isWarnEnabled())
        		log.warn("Exception for url="+url, exception);
			//let's stop processing all streams on failure...
			throw new RuntimeException("failure", exception);
		}
		
		public Throwable getCause() {
			return cause;
		}

		public void setCause(Throwable cause) {
			this.cause = cause;
		}

	}
}
