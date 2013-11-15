package controllers.gui;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;
import controllers.gui.util.Line;

public class SaveBatch implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(SaveBatch.class);
	private static final int FLUSH_SIZE = 500;
	private List<DboColumnMeta> headers;
	private int headersize = 0;
	private Line[] batch;
	private Request request;
	private DboTableMeta table;
	private int count = 0;
	private LinkedHashMap<String, Integer> headerIndexs = new LinkedHashMap<String, Integer>();
	
	//this are precaculated outside the run loop for performance:
	private int batchsize;
	private String tableColumnFamily;

	
	//jsc for performance tracking:
	private long flushCount = 0;
	private long accumulatedFlushTime = 0;

	public SaveBatch(DboTableMeta table, List<DboColumnMeta> headers, Line[] batch2, Request request, int batchsize) {
		this.table = table;
		this.headers = headers;
		this.headersize = headers.size();
		this.batch = batch2;
		this.request = request;
		this.batchsize = batchsize;
		this.tableColumnFamily = table.getColumnFamily();
		
		for (int i = 0; i < headers.size(); i++) {
			headerIndexs.put(headers.get(i).getColumnName(), i);
		}
		
	}

	@Override
	public void run() {
		try {
			runImpl();
			
			synchronized(request) {
				Integer count = (Integer) request.args.get("count");
				int newCount = count+batchsize;
				Integer total = (Integer) request.args.get("total");
				request.args.put("count", newCount);
				if(log.isDebugEnabled())
					log.debug("csv upload - notify chunk complete.  count completed="+newCount+" of total="+total);
				request.notifyAll();
			}

		} catch(Exception e) {
			request.args.put("error", e);
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception processing batch.", e);
			reportError(request, "Error processing batch="+e);
		}
		
	}

	private static void reportError(Request request, String msg) {
		Map<String, Object> map = request.args;
		Object obj = map.get("errorsList");
		List<String> errors = (List<String>) obj;
		if(errors.size() < Tables.ERROR_LIMIT) {
			errors.add(msg);
		}
	}
	
	private void runImpl() {
		NoSqlEntityManagerFactory factory = (NoSqlEntityManagerFactory) request.args.get("factory");
		NoSqlEntityManager mgr = factory.createEntityManager();
		
		long preLoop = System.currentTimeMillis();
		NoSqlTypedSession session = mgr.getTypedSession();
		for (int i = 0; i<batchsize; i++) {
		//for(Line line : batch) {
			Line line = batch[i];
			if(request.args.get("error") != null) {
				if(log.isInfoEnabled())
					log.info("csv upload - Exiting since another thread had an error");
				return; //exit out since somoene errored
			}
			processLine(mgr, line, session);
			count++;
		}
		if (log.isDebugEnabled())
			log.debug("time to run full loop : "+(System.currentTimeMillis()-preLoop));

		mgr.flush();
		mgr.clear();
	}

	private void processLine(NoSqlEntityManager mgr, Line line, NoSqlTypedSession session) {
		if(table.isTimeSeries())
			processLineTimeSeries(mgr, line, session);
		else
			processLineRel(mgr, line, session);
	}

	private void processLineTimeSeries(NoSqlEntityManager mgr, Line line, NoSqlTypedSession session) {
		try {
//			BigInteger time = null;
//			Object value = null;
//			for(int i = 0; i < headersize; i++) {
//				String col = line.getColumns()[i];
//				DboColumnMeta meta = headers.get(i);
//				Object val = ApiPostDataPointsImpl.convertToStorage(meta, col.trim());
//				if(meta instanceof DboColumnIdMeta)
//					time = (BigInteger) val;
//				else
//					value = val;
//			}
//			ApiPostDataPointsImpl.postTimeSeriesImpl(mgr, table, time, value, false);
//			
			
			
			
			List<DboColumnMeta> cols = new ArrayList<DboColumnMeta>(table.getAllColumns());
			List<Object> nodes = new ArrayList<Object>();
			String pkValue = line.getColumns()[headerIndexs.get("time")];
			if (pkValue == null)
				throw new RuntimeException("The time cannot be null!");
			//workaround for scientific notation which can't currently be parsed by playorm:
			if (StringUtils.containsIgnoreCase(pkValue, "e")) {
				pkValue = ""+Double.valueOf(pkValue).longValue();
			}
			for (DboColumnMeta col:cols) {
				String node = line.getColumns()[headerIndexs.get(col.getColumnName())];
				if(node == null) {
					if (log.isWarnEnabled())
						log.warn("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in data");
					throw new BadRequest("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in data");
				}
				if(!(col instanceof DboColumnIdMeta))
					nodes.add(node.trim());
			}
			
			if (cols.size() > 1) {
				Map<DboColumnMeta, Object> newValue = ApiPostDataPointsImpl.convertToStorage(cols, nodes);
				ApiPostDataPointsImpl.postRelationalTimeSeriesImpl(mgr, table, pkValue, newValue, false);
			}
			else {
				Object newValue = ApiPostDataPointsImpl.convertToStorage(cols.get(0), nodes.get(0));
				ApiPostDataPointsImpl.postTimeSeriesImpl(mgr, table, pkValue, newValue, false);
			}
			
			
			
			

		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception on line num="+line.getLineNumber(), e);
			reportError(request, "Exception processing line number="+line.getLineNumber()+" exc="+e.getMessage());
		}
		
		if(count >= FLUSH_SIZE) {
			flushCount++;
			long preFlush = System.currentTimeMillis();
			mgr.flush();
			long postFlush = System.currentTimeMillis();
			mgr.clear();
			count=0;
			if (log.isDebugEnabled()) {
				accumulatedFlushTime+=System.currentTimeMillis()-preFlush;
				log.debug("flush count: "+flushCount+", time to flush once: "+(postFlush-preFlush)+ ", time to clear "+(System.currentTimeMillis()-postFlush)+" total flush time: "+accumulatedFlushTime);
			}
		}
	}

	private void processLineRel(NoSqlEntityManager mgr, Line line, NoSqlTypedSession session) {
		try {
			TypedRow row = session.createTypedRow(tableColumnFamily);
			for(int i = 0; i < headersize; i++) {
				String col = line.getColumns()[i];
				DboColumnMeta meta = headers.get(i);
				Object val = ApiPostDataPointsImpl.convertToStorage(meta, col.trim());
				if(meta instanceof DboColumnIdMeta)
					row.setRowKey(val);
				else
					row.addColumn(meta.getColumnName(), val);
			}
	
			session.put(tableColumnFamily, row);

		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception on line num="+line.getLineNumber(), e);
			reportError(request, "Exception processing line number="+line.getLineNumber()+" exc="+e.getMessage());
		}
		
		if(count >= FLUSH_SIZE) {
			flushCount++;
			long preFlush = System.currentTimeMillis();
			mgr.flush();
			long postFlush = System.currentTimeMillis();
			mgr.clear();
			count=0;
			if (log.isDebugEnabled()) {
				accumulatedFlushTime+=System.currentTimeMillis()-preFlush;
				log.debug("flush count: "+flushCount+", time to flush once: "+(postFlush-preFlush)+ ", time to clear "+(System.currentTimeMillis()-postFlush)+" total flush time: "+accumulatedFlushTime);
			}
		}
	}

}
