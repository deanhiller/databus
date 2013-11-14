package controllers;

import gov.nrel.util.Utility;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import models.DataTypeEnum;
import models.SecureTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F.Promise;
import play.mvc.Controller;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.gui.util.ExecutorsSingleton;

public class ApiGetData extends Controller {
	//DO NOT make final!!! as then when you change batch size it has no effect since the other classes are not recompiled!!!
	public static int BATCH_SIZE = 200;
	//private static final String PREFIX = "keyToTable-";
	static final Logger log = LoggerFactory.getLogger(ApiGetData.class);

	

	public static void getData(String table, String from, String to) {
		String user = request.user;
		if (log.isInfoEnabled())
			log.info("user="+user+" requesting data on table="+table);
		SecureTable sdiTable = SecurityUtil.checkSingleTable(table);
		
		try {
			
			DataTypeEnum type = sdiTable.getTypeOfData();
			if(type != DataTypeEnum.TIME_SERIES && type != DataTypeEnum.RELATIONAL_TIME_SERIES)
				badRequest("This table is not a time series table.  Need the ad-hoc interface for this.");
			
			long start = Long.parseLong(from);
			long end = Long.parseLong(to);
			if(start > end)
				notFound("start time cannot be larger than end time");
			
			process(table, start, end);
		} catch(NumberFormatException e) {
			notFound("start time or end time was not of type integer");
		}
	}

	private static void process(String colFamily, long start, long end) {
		//push this down into the raw module once we are done here...
		StreamRegisterCallback streamer;
		if(request.isNew) {
			//If this is the first time in here, it is a new request
			streamer = setupForRequest(colFamily, start, end);
			request.args.put("streamer", streamer);
		} else {
			//if this is the second, third, etc. time in here, it is a continuation of
			//the previous request
			streamer = (StreamRegisterCallback) request.args.get("streamer");
		}
		
	    while(streamer.hasMoreData() && !streamer.hasError()) {
	    	Promise<String> promise = streamer.fetchDataAsynchronously(BATCH_SIZE);
	    	await(promise);
	    	String nextChunk = fetchNext(promise);
	    	response.writeChunk(nextChunk);
	    }
	    
	    response.writeChunk("] }");
	    if (log.isInfoEnabled())
			log.info("finished getdata for="+colFamily+" start="+start+" end="+end);
	}

	private static String fetchNext(Promise<String> promise) {
		try {
			return promise.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private static StreamRegisterCallback setupForRequest(String colFamily,
			long start, long end) {
		NoSqlEntityManager em = NoSql.em();

		SecureTable sdiTable = SecureTable.findByName(NoSql.em(), colFamily);
		
		DboTableMeta table = sdiTable.getTableMeta();
		String cf = table.getColumnFamily();
		String idCol = table.getIdColumnMeta().getColumnName();
		String sql = "SELECT c FROM "+cf+" as c WHERE c."+idCol+" >= "+start+" and c."+idCol+" <= "+end;
		if (log.isInfoEnabled())
			log.info("Setting up for sql="+sql);
		NoSqlTypedSession typedSession = em.getTypedSession();
		QueryResult result = typedSession.createQueryCursor(sql, 500);
		Cursor<KeyValue<TypedRow>> cursor = result.getPrimaryViewCursor();
		Iterable<KeyValue<TypedRow>> proxy = Utility.fetchProxy(cursor);
		
	    StreamRegisterCallback streamer = new StreamRegisterCallback(sql, proxy.iterator(), table);
		response.contentType = "application/json";
		
		response.writeChunk("{ \"data\": [");
		return streamer;
	}

	public static class StreamRegisterCallback {
		private Promise<String> lastPromise;
		private ApiOurJsonStreamer lastStreamer;
		private Iterator<KeyValue<TypedRow>> rows;
		private DboTableMeta tableMeta;
		private String sql;

		public StreamRegisterCallback(String sql, Iterator<KeyValue<TypedRow>> iterator, DboTableMeta tableMeta2) {
			this.sql = sql;
			this.rows = iterator;
			this.tableMeta = tableMeta2;
		}

		public boolean hasError() {
			if(lastStreamer == null)
				return false;
			return lastStreamer.hasError();
		}

		public Promise<String> fetchDataAsynchronously(int batchSize) {
			lastPromise = new Promise<String>();
			int count = 0;
			if(lastStreamer != null)
				count = lastStreamer.getCount();
			lastStreamer = new ApiOurJsonStreamer(sql, tableMeta, rows, lastPromise, count, batchSize);
			if (log.isInfoEnabled())
				log.info("launching exec for="+sql);
			ExecutorsSingleton.executor.execute(lastStreamer);
			return lastPromise;
		}

		public boolean hasMoreData() {
			return rows.hasNext();
		}
	}
	
//	@BasicAuth
//	public static Result getHello() {
//		if (log.isInfoEnabled())
//			log.info("getting hello");
//		ObjectNode result = Json.newObject();
//		result.put("last", "BASIC AUTH NOT WORKING");
//		result.put("name", "some name somewhere");
//		return ok(result);
//	}
}
