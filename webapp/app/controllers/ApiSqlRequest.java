package controllers;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import models.RoleMapping;
import models.SecureResourceGroupXref;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Response;
import play.mvc.Util;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.exc.ParseException;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z5api.IndexColumnInfo;
import com.alvazan.orm.api.z5api.IndexPoint;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.orm.api.z8spi.meta.ViewInfo;
import com.alvazan.play.NoSql;

import controllers.gui.util.ExecutorsSingleton;
import controllers.modules.util.ModuleStreamer;

public class ApiSqlRequest extends Controller {

	public static final Logger log = LoggerFactory.getLogger(ApiSqlRequest.class);
	
	public static void timeSlice(String table, Long start, Long end) throws InterruptedException, ExecutionException {
		if(start > end)
			notFound("start time cannot be larger than end time");
		
		String cf = table.replaceAll("\\s","");
		String sql = "select d from "+cf+" as d where d.time >= "+start+" and d.time <= "+end;

	    String param = request.params.get("callback");
	    
		//OKAY, at this point, we are ready to rock and roll
		StreamSqlResultCallback streamer = null;
		String tempTotal = "";
		if(request.isNew) {
			//If this is the first time in here, it is a new request
			String correctSql = sql.replace("+", " ");
			QueryResult result = null;
			try {
				result = securityChecks(correctSql);
			}
			catch (Throwable e) {
				streamer = new StreamSqlResultCallback(request.user, request.password, correctSql, null);
				ModuleStreamer.writeHeader(param);
				streamer.setCause(e);
			}

			if (streamer == null) {
				Cursor<List<TypedRow>> cursor = result.getAllViewsCursor();
				Iterable<List<TypedRow>> iterable = Utility.fetchProxy(cursor);
				streamer = setupForRequest(request.user, request.password, correctSql, iterable, param);
			}
			request.args.put("streamer", streamer);
			request.args.put("time", System.currentTimeMillis());
			request.args.put("tempTotal", tempTotal);
		} else {
			//if this is the second, third, etc. time in here, it is a continuation of
			//the original request
			streamer = (StreamSqlResultCallback) request.args.get("streamer");
			tempTotal = (String) request.args.get("tempTotal");
		}

		int batchSize = ApiGetData.BATCH_SIZE;
	    while(!streamer.hasError() && streamer.hasMoreData()) {
	    	Promise<String> promise = streamer.fetchDataAsynchronously(batchSize);
	    	await(promise);
	    	String chunk = promise.get();
	    	tempTotal+=chunk;
	    	request.args.put("tempTotal", tempTotal);
	    	Response response = Response.current();
			response.writeChunk(chunk);
	    }

	    if(log.isDebugEnabled())
	    	log.debug(tempTotal);
	    
	    ModuleStreamer.writeFooter(param, streamer.getCause());

	    Long startTime = (Long) request.args.get("time");
	    long total = System.currentTimeMillis() - startTime;
	    if (log.isInfoEnabled())
			log.info("finished with request="+sql+" total time="+total+" ms");
	}

	public static void sqlRequest(String sql) throws InterruptedException, ExecutionException {
		
	    String param = request.params.get("callback");
	    
		//OKAY, at this point, we are ready to rock and roll
		StreamSqlResultCallback streamer;
		if(request.isNew) {
			//If this is the first time in here, it is a new request
			String correctSql = sql.replace("+", " ");
			QueryResult result = securityChecks(correctSql);
			Iterable<List<TypedRow>> cursor = result.getAllViewsIter();

			streamer = setupForRequest(request.user, request.password, correctSql, cursor, param);
			request.args.put("streamer", streamer);
			request.args.put("time", System.currentTimeMillis());
		} else {
			//if this is the second, third, etc. time in here, it is a continuation of
			//the original request
			streamer = (StreamSqlResultCallback) request.args.get("streamer");
		}

		int batchSize = ApiGetData.BATCH_SIZE;
	    while(streamer.hasMoreData() && !streamer.hasError()) {
	    	Promise<String> promise = streamer.fetchDataAsynchronously(batchSize);
	    	await(promise);
	    	String chunk = promise.get();
	    	response.writeChunk(chunk);
	    }

	    ModuleStreamer.writeFooter(param, streamer.getCause());

	    Long start = (Long) request.args.get("time");
	    long total = System.currentTimeMillis() - start;
	    if (log.isInfoEnabled())
			log.info("finished with request="+sql+" total time="+total+" ms");
	}
	@Util
	private static QueryResult securityChecks(String sql) {
		try {
			return securityChecksImpl(sql);
		} catch(ParseException e) {
			Throwable childExc = e.getCause();
			if (log.isInfoEnabled())
				log.info("parse exception", e);
			badRequest("Your sql is invalid.  reason="+childExc.getMessage());
			return null; //above badRequest throws exception so doesn't matter
		}
	}
	
	private static QueryResult securityChecksImpl(String sql) {		
		NoSqlEntityManager em = NoSql.em();
		NoSqlTypedSession s = em.getTypedSession();
		
		QueryResult result = s.createQueryCursor(sql, ApiGetData.BATCH_SIZE);

		Map<String, SecureResourceGroupXref> schemaIds = SecurityUtil.groupSecurityCheck();
		List<ViewInfo> views = result.getViews();
		
		//check access for every table in every group
		for(ViewInfo view : views) {
			String cf = view.getTableMeta().getColumnFamily();
			SecurityUtil.checkTable(schemaIds, cf);
		}
		return result;
	}

	private static StreamSqlResultCallback setupForRequest(String user, String key, String sql, Iterable<List<TypedRow>> cursor, String param) {
		StreamSqlResultCallback streamer = new StreamSqlResultCallback(user, key, sql, cursor.iterator());
		ModuleStreamer.writeHeader(param);
		return streamer;
	}
	
	public static class StreamSqlResultCallback {
		private Iterator<List<TypedRow>> iterator;
		private Promise<String> lastPromise;
		private OurSqlJsonStreamer lastStreamer;
		private String sql;
		private String user;
		private String key;
		private Throwable cause=null;

		public StreamSqlResultCallback(String user, String key, String sql, Iterator<List<TypedRow>> cursor) {
			this.user = user;
			this.key = key;
			this.sql = sql;
			this.iterator = cursor;
		}

		public boolean hasError() {
			if (cause != null)
				return true;
			if(lastStreamer == null)
				return false;
			return lastStreamer.hasError();
		}
		
		public Throwable getCause() {
			if (cause != null)
				return cause;
			if (lastStreamer == null)
				return null;
			return lastStreamer.getCause();
		}
		
		public void setCause(Throwable cause) {
			this.cause = cause;
		}

		public Promise<String> fetchDataAsynchronously(int batchSize) {
			lastPromise = new Promise<String>();
			int count = 0;
			if(lastStreamer != null)
				count = lastStreamer.getCount();
			lastStreamer = new OurSqlJsonStreamer(sql, iterator, lastPromise, count, batchSize);
			lastStreamer.setUser(user);
			lastStreamer.setKey(key);
			if (log.isInfoEnabled())
				log.info("launching exec for="+sql);
			ExecutorsSingleton.executor.execute(lastStreamer);
			return lastPromise;
		}

		public boolean hasMoreData() {
			return iterator.hasNext();
		}
	}
	
	public static class OurSqlJsonStreamer implements Runnable {

		private String sql;
		private Iterator<List<TypedRow>> iterator;
		private Promise<String> lastPromise;
		private int count;
		private int batchSize;
		private String user;
		private String key;
		private boolean hasError;
		private Throwable cause=null;

		
		public OurSqlJsonStreamer(String sql, Iterator<List<TypedRow>> iterator,
				Promise<String> lastPromise, int count, int batchSize) {
			this.sql = sql;
			this.iterator = iterator;
			this.lastPromise = lastPromise;
			this.count = count;
			this.batchSize = batchSize;
		}

		public boolean hasError() {
			return hasError;
		}
		
		public Throwable getCause() {
			return cause;
		}

		public void setCause(Throwable cause) {
			this.cause = cause;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public int getCount() {
			return count;
		}

		@Override
		public void run() {
			try {
				runImpl();
			} catch(Exception e) {
				if (log.isWarnEnabled())
	        		log.warn("Exception streaming back file for user="+user+" and key="+key, e);
				setCause(e);
				hasError = true;
				lastPromise.invoke("BUG, FAILURE, due to exception. msg="+e.getMessage());
			}
		}

		private void runImpl() {
			if (log.isInfoEnabled())
				log.info("starting to run sql="+sql+" count="+count);
			
			//NoSqlEntityManager mgr = NoSql.em();
			//NoSqlTypedSession typedSession = mgr.getTypedSession();
			/*
			{ "data": [
				  [
				   { "firstName":"John" , "lastName":"Doe" }, 
				   { "firstName":"Anna" , "lastName":"Smith" }, 
				   { "firstName":"Peter" , "lastName":"Jones" }
				  ],
			      [
				   { "firstName":"John" , "lastName":"Doe" }, 
				   { "firstName":"Anna" , "lastName":"Smith" }, 
				   { "firstName":"Peter" , "lastName":"Jones" }			      
			      ]
			    ]
			}
*/
			String chunkOfData = "";
			int chunkCounter = 0;
			int size = 0;
			while(iterator.hasNext() && chunkCounter < batchSize) {
				List<TypedRow> typedRows = iterator.next();
				size = typedRows.size();
				String json = translate(typedRows);
				chunkOfData += json;
				if(iterator.hasNext()) {
					//for the very last row of data(last row in last batch), 
					//we must write it out with no comma at the end
					chunkOfData += ",";
				}
				chunkCounter++;
			}

			count += chunkCounter;
			if (log.isDebugEnabled())
				log.debug("Total processed so far="+count+" num processed this chunk="+chunkCounter+" rownumInRow="+size);
			lastPromise.invoke(chunkOfData);
		}

		private String translate(List<TypedRow> typedRows) {
			String joinedRow = "";
			for(int i = 0; i < typedRows.size(); i++) {
				TypedRow row = typedRows.get(i);
				DboTableMeta meta = row.getView().getTableMeta();
				joinedRow += ApiOurJsonStreamer.translate(meta, row);
				if(i != typedRows.size()-1) {
					joinedRow += ",";
				}
			}
			
			if(typedRows.size() == 1)
				return joinedRow;
			
			return "["+joinedRow+"]";
		}

//		private String simpleTranslate(List<TypedRow> typedRows) {
//			if(typedRows.size() != 1)
//				throw new RuntimeException("Can't do joins with simple json");
//			TypedRow row = typedRows.get(0);
//			String keyStr = row.getRowKeyString();
//			TypedColumn key = row.getColumn("time");
//			TypedColumn val = row.getColumn("value");
//			String valStr = val.getValueAsString();
//			
//			return "\""+keyStr+"\":"+valStr;
//		}
		
	}
}
