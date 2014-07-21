package controllers.impl;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;
import controllers.modules2.RawTimeSeriesProcessor;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.RowMeta;

public class RawTimeSeriesImpl {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesImpl.class);

	protected DboTableMeta meta;
	protected byte[] endBytes;
	protected byte[] startBytes;
	protected Long currentPartitionId;
	protected Long start;
	protected Long end;
	protected AbstractCursor<Column> cursor;
	protected Long partitionSize;
	private List<DboColumnMeta> colMeta = new ArrayList<DboColumnMeta>();
	protected int currentIndex;
	protected List<Long> existingPartitions = new ArrayList<Long>();
	protected NoSqlEntityManager mgr;

	private String timeColumn;
	private String valueColumn;

	public void init(DboTableMeta meta, Long start, Long end, RowMeta rowMeta, NoSqlEntityManager mgr) {
		this.mgr = mgr;
		this.meta = meta;
		
		this.timeColumn = rowMeta.getTimeColumn();
		this.valueColumn = rowMeta.getValueColumn();
		
		this.start = start;
		this.end = end;
		loadPartitions(meta);
		partitionSize = meta.getTimeSeriesPartionSize();
		currentIndex = partition(partitionSize);

		if(start != null)
			this.startBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(start+""));
		if(end != null)
			this.endBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(end+""));

		for (DboColumnMeta thismeta:meta.getAllColumns())
			colMeta.add(thismeta);
		if (log.isInfoEnabled())
			log.info("Setting up for reading partitions, partId="+currentIndex+" partitions="+existingPartitions+" start="+start);
	}
	
	public void deleteRange() {
		int flushSize = 1000;
		NoSqlTypedSession s = mgr.getTypedSession();
		
		TSRelational ts = nextResult();
		long startTime = System.currentTimeMillis();
		long count = 0;
		while (ts != null) {
			//slow way:  get row, delete column.
			TypedRow partition = getCurrentPartition();
			partition.removeColumn(meta.getIdColumnMeta().convertToStorage2(ApiPostDataPointsImpl.convertToStorage(meta.getIdColumnMeta(), ""+ts.getTime())));
			//partition.removeColumn(""+ts.getTime());
			s.put(meta.getColumnFamily(), partition);
			if (count%flushSize == 0) {
				log.info("flushing, count is "+count);
				s.flush();
				mgr.flush();
			}
			
			//fast way:  figure out if this row (partition) will have all of it's columns deleted in this operation, if so, just delete the entire row
			count++;
			ts = nextResult();
		}
		s.flush();
		mgr.flush();
		long endTime = System.currentTimeMillis();
		if (count == 0) log.info("no items to delete in the range "+start+" to "+end);
		else log.info("deleted "+count+" items, total time "+(endTime-startTime)+"ms, "+((endTime-startTime)/count)+"ms per item deleted");		
		
	}

	private void loadPartitions(DboTableMeta meta2) {
		DboTableMeta tableMeta = mgr.find(DboTableMeta.class, "partitions");
		NoSqlSession session = mgr.getSession();
		byte[] rowKey = StandardConverters.convertToBytes(meta2.getColumnFamily());
		Cursor<Column> results = session.columnSlice(tableMeta, rowKey, null, null, 1000, BigInteger.class);
		
		while(results.next()) {
			Column col = results.getCurrent();
			BigInteger time = StandardConverters.convertFromBytes(BigInteger.class, col.getName());
			existingPartitions.add(time.longValue());
		}
		
		Collections.sort(existingPartitions);
	}

	protected int partition(long partitionSize) {
		if(existingPartitions.size() > 0 && start == null)
			return 0;
		
		for(int i = 0; i < existingPartitions.size();i++) {
			long partId = existingPartitions.get(i);
			if(start < partId+partitionSize)
				return i;
		}

		return existingPartitions.size();
	}

	
	public TSRelational nextResult() {
		AbstractCursor<Column> cursor = getCursorWithResults();
		if(cursor == null)
			return null; //no more data to read

		return translate(cursor.getCurrent());

	}


	protected AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentIndex >= existingPartitions.size())
			return null;

		do {
			currentPartitionId = existingPartitions.get(currentIndex);
			NoSqlTypedSession em = mgr.getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartitionId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 500, BigInteger.class);
			currentIndex++;
			if(cursor.next())
				return cursor;

		} while(!hasReachedEnd(end));

		//reached end and no data found
		return null;
	}
	
	public TypedRow getCurrentPartition() {
		NoSqlTypedSession typedSession = mgr.getTypedSession();
		TypedRow row = typedSession.createTypedRow(meta.getColumnFamily());
		BigInteger rowKey = new BigInteger(""+currentPartitionId);
		row.setRowKey(rowKey);
		return row;
	}

	private boolean hasReachedEnd(Long end) {
		if(currentIndex >= existingPartitions.size())
			return true;
		Long partId = existingPartitions.get(currentIndex);
		if(end < partId)
			return true;
		return false;
	}

	private TSRelational translate(Column current) {
		byte[] name = current.getName();
		byte[] value = current.getValue();
		Object time = meta.getIdColumnMeta().convertFromStorage2(name);
		if (colMeta.size() == 1 && "value".equals(colMeta.get(0).getColumnName()))
			return translateTS(current, time, value);
		else 
			return translateRelationalTS(current, time, value);

		
	}

	private TSRelational translateRelationalTS(Column current, Object time, byte[] value) {
		TSRelational tv = new TSRelational();
		tv.put(timeColumn, time);
		if (value!=null && value.length!=0) {
			int startindex = 0;
			int endindex;
			for (DboColumnMeta meta:colMeta) {
				int len = ByteBuffer.wrap(Arrays.copyOfRange(value, startindex, startindex+4)).getInt();
				//int len = (int)value[startindex];
				endindex = startindex+4+len;
				byte[] thisval = Arrays.copyOfRange(value, startindex+4, endindex);
				Object val = meta.convertFromStorage2(thisval);
				tv.put(meta.getColumnName(), val);
				startindex=endindex;
			}
		}
		return tv;
	}

	private TSRelational translateTS(Column current, Object time, byte[] value) {
		Object val = colMeta.get(0).convertFromStorage2(value);
		//TODO:  parameterize timeColumn and valueColumn from options
		TSRelational tv = new TSRelational();
		tv.put(timeColumn, time);
		tv.put(valueColumn, val);
		return tv;
	}

}
