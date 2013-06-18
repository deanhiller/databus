package controllers.gui;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;

import controllers.ApiPostDataPointsImpl;

public class SaveBatch implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(SaveBatch.class);
	private static final int FLUSH_SIZE = 500;
	private List<DboColumnMeta> headers;
	private int headersize = 0;
	private Line[] batch;
	private Request request;
	private DboTableMeta table;
	private int count = 0;
	
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
