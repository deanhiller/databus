package controllers.gui;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Outbound;

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
	private List<Line> batch;
	private DboTableMeta table;
	private int count = 0;
	
	//this are precaculated outside the run loop for performance:
	private String tableColumnFamily;
	
	private long currentPartitionStart = 0;
	private long currentPartitionEnd = 0;

	
	//jsc for performance tracking:
	private long flushCount = 0;
	private long accumulatedFlushTime = 0;
	private SocketState state;
	private int characterCount;
	private Outbound outbound;

	public SaveBatch(DboTableMeta table, List<Line> batch2, SocketState state, Outbound outbound) {
		this.table = table;
		this.batch = batch2;
		this.state = state;
		this.outbound = outbound;
		this.tableColumnFamily = table.getColumnFamily();
		
	}

	@Override
	public void run() {
		try {
			//record in case someone reduces size of list later
			int batchsize = batch.size();
			
			runImpl();
			
			if(state.getErrors().size() > 0)
				return; //exit out since someone else reported and error
				
			synchronized(state) {
				int count = state.getDoneCount();
				int newCount = count+batchsize;
				int numSubmitted = state.getNumItemsSubmitted();
				state.setDoneCount(newCount);
				state.addTotalCharactersDone(characterCount);
				outbound.send("{ \"numdone\":"+state.getCharactersDone()+"}");
				if(state.getCharactersDone() >= state.getFileSize()) {
					log.info("We are done");
					outbound.send("{ \"done\": true }");
				}

				if(log.isInfoEnabled())
					log.info("csv upload - notify chunk complete.  count completed="+newCount+"/"+numSubmitted+" chars="+state.getCharactersDone()+"/"+state.getFileSize());
			}

		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception processing batch.", e);
			reportError("Error processing batch. exception="+e+" msg="+e.getMessage());
		}
	}

	private void reportError(String msg) {
		state.reportErrorAndClose(msg);
	}

	private void runImpl() {
		NoSqlEntityManagerFactory factory = state.getEntityManagerFactory();
		NoSqlEntityManager mgr = factory.createEntityManager();
		
		long preLoop = System.currentTimeMillis();
		NoSqlTypedSession session = mgr.getTypedSession();
		for(Line line : batch) {
			if(state.getErrors().size() > 0) {
				if(log.isDebugEnabled())
					log.debug("csv upload - Exiting since another thread had an error. first="+state.getErrors().get(0));
				return; //exit out since somoene errored
			}
			processLine(mgr, line, session);

			characterCount += line.getLength();
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
			List<DboColumnMeta> cols = new ArrayList<DboColumnMeta>(table.getAllColumns());
			List<Object> nodes = new ArrayList<Object>();
			String pkValue = line.getColumns().get("time");
			if (pkValue == null)
				throw new RuntimeException("The time cannot be null!");
			//workaround for scientific notation which can't currently be parsed by playorm:
			if (StringUtils.containsIgnoreCase(pkValue, "e")) {
				pkValue = ""+Double.valueOf(pkValue).longValue();
			}
			
			for (DboColumnMeta col:cols) {
				String node = line.getColumns().get(col.getColumnName());
				if(node == null) {
					if (log.isWarnEnabled())
						log.warn("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in data");
					throw new RuntimeException("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in data");
				}
				if(!(col instanceof DboColumnIdMeta))
					nodes.add(node.trim());
			}
			createPartitionIfNeeded(mgr, pkValue);
			if (cols.size() > 1) {
				Map<DboColumnMeta, Object> newValue = ApiPostDataPointsImpl.convertToStorage(cols, nodes);
				ApiPostDataPointsImpl.postRelationalTimeSeriesImpl(mgr, table, pkValue, newValue, false);
			}
			else {
				Object newValue = ApiPostDataPointsImpl.convertToStorage(cols.get(0), nodes.get(0));
				//ApiPostDataPointsImpl.postTimeSeriesImpl(mgr, table, pkValue, newValue, false);
				ApiPostDataPointsImpl.postTimeSeriesImplAssumingPartitionExists(mgr, table, pkValue, newValue, false, currentPartitionStart);
			}

		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception on line num="+line.getLineNumber(), e);
			reportError("Exception processing line number="+line.getLineNumber()+" exc="+e.getMessage());
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

	private void createPartitionIfNeeded(NoSqlEntityManager mgr, String pkVal) {
		long timestamp = Long.valueOf(pkVal);
		if (! (currentPartitionStart <= timestamp && timestamp <= currentPartitionEnd)) {
			currentPartitionStart = ApiPostDataPointsImpl.calculatePartitionId(timestamp, table.getTimeSeriesPartionSize());
			currentPartitionEnd = currentPartitionStart + table.getTimeSeriesPartionSize();
			ApiPostDataPointsImpl.putPartition(mgr, table, currentPartitionStart);
		}
	}

	private void processLineRel(NoSqlEntityManager mgr, Line line, NoSqlTypedSession session) {
		try {
			TypedRow row = session.createTypedRow(tableColumnFamily);
			for (String colname:line.getColumns().keySet()) {
				DboColumnMeta meta = table.getColumnMeta(colname);
				String stringval = line.getColumns().get(colname);
				//jsc:todo  Do we need to do a 'required' check here and throw an exception in some cases for blank???
				if (StringUtils.isBlank(stringval))
					continue;
				Object val = ApiPostDataPointsImpl.convertToStorage(meta, stringval.trim());
				if(meta instanceof DboColumnIdMeta)
					row.setRowKey(val);
				else
					row.addColumn(meta.getColumnName(), val);

			}
	
			session.put(tableColumnFamily, row);

		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("csv upload - Exception on line num="+line.getLineNumber(), e);
			reportError("Exception processing line number="+line.getLineNumber()+" exc="+e.getMessage());
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
