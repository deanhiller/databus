package controllers.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.PlayPlugin;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.conv.StorageTypeEnum;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
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

public class RawTimeSeriesImpl  extends PlayPlugin {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesImpl.class);

	public static final TSRelational END_OF_DATA_TOKEN = new TSRelational();
	
	protected DboTableMeta meta;
	protected DboColumnIdMeta idcolMeta;

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
	protected BlockingQueue<TSRelational> itemsQueue;

	private String timeColumn;
	private String valueColumn;
	
	private final static Charset charset = StandardCharsets.UTF_8;

	public void init(DboTableMeta meta, Long start, Long end, RowMeta rowMeta, NoSqlEntityManager mgr) {
		this.mgr = mgr;
		this.meta = meta;
		idcolMeta = meta.getIdColumnMeta();

		
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
		
		//jsc fire off read thread
		itemsQueue = new LinkedBlockingQueue<TSRelational>(1000);
		new Thread(new AgressivePreReader()).start();
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
			partition.removeColumn(meta.getIdColumnMeta().convertToStorage2(ApiPostDataPointsImpl.stringToObject(meta.getIdColumnMeta().getStorageType(), ""+ts.getTime())));
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
		if (log.isDebugEnabled()) {
			long endTime = System.currentTimeMillis();
			if (count == 0) log.debug("no items to delete in the range "+start+" to "+end);
			else log.debug("deleted "+count+" items, total time "+(endTime-startTime)+"ms, "+((endTime-startTime)/count)+"ms per item deleted");
		}
		
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

	private int count = 0;
	private boolean datafinished = false;
	public TSRelational nextResult() {
		if (datafinished)
			return null;
		try {
			TSRelational next = itemsQueue.take();
			if (count %100 == 0)
				System.out.println("Removing an item, the size of the queue is "+itemsQueue.size()+" we've loaded "+count+" items");
			count++;
			
			if (next == END_OF_DATA_TOKEN) {
				datafinished = true;
				return null;
			}
			return next;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

	}


	protected AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentIndex >= existingPartitions.size())
			return null;
		NoSqlTypedSession em = mgr.getTypedSession();
		NoSqlSession raw = em.getRawSession();

		do {
			currentPartitionId = existingPartitions.get(currentIndex);

			byte[] rowKeyPostFix = idcolMeta.convertToStorage2(new BigInteger(""+currentPartitionId));
			byte[] rowKey = idcolMeta.formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 1000, BigInteger.class);
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
		//this is the same as the next line, except the next line is WAY faster
		//Object time = meta.getIdColumnMeta().convertFromStorage2(name);
		Object time = fromBytes(StorageTypeEnum.INTEGER, name, null);
		if (colMeta.size() == 1 && "value".equals(colMeta.get(0).getColumnName()))
			return translateTS(current, time, value);
		else 
			return translateRelationalTS(current, time, value);
		
	}
	
	private Object fromBytes(StorageTypeEnum type, byte[] source, DboColumnMeta fallbackmeta) {
		if(source == null)
			return null;
		else if(source.length == 0)
			return null;
		switch (type) {
			case STRING: return new String(source, charset);
			case INTEGER: return new BigInteger(source);
			case DECIMAL: 	ByteBuffer buf = ByteBuffer.wrap(source);
							int scale = buf.getInt();
							byte[] bibytes = new byte[buf.remaining()];
							buf.get(bibytes);
							 
							BigInteger bi = new BigInteger(bibytes);
							return new BigDecimal(bi,scale);
			default: return fallbackmeta.convertFromStorage2(source);
		}
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
				//the following two lines are identical, but the umcommented one is WAY faster
				//Object val = meta.convertFromStorage2(thisval);
				Object val = fromBytes(meta.getStorageType(), thisval, meta);
				tv.put(meta.getColumnName(), val);
				startindex=endindex;
			}
		}
		return tv;
	}

	private TSRelational translateTS(Column current, Object time, byte[] value) {
		
		return new TSRelational((BigInteger)time, fromBytes(colMeta.get(0).getStorageType(), value, colMeta.get(0)));
	}
	
	private class AgressivePreReader extends PlayPlugin implements Runnable  {
		
		private int count = 0;
		@Override
		public void run() {
			TSRelational nextItem = nextResult();
			while (nextItem != null) {
				try {
					itemsQueue.put(nextItem);
					nextItem = nextResult();
					if (count %100 == 0)
						System.out.println("Adding an item, the size of the queue is "+itemsQueue.size()+" we've loaded "+count+" items");
					count++;
				}
				catch (InterruptedException ie) {
					ie.printStackTrace();
					break;
				}
			}
			try {
				itemsQueue.put(END_OF_DATA_TOKEN);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		
		public TSRelational nextResult() {
			AbstractCursor<Column> cursor = getCursorWithResults();
			if(cursor == null) {
				System.out.println("cursor ran out, returning null");
				return null; //no more data to read
			}

			return translate(cursor.getCurrent());
		}
		
	}

}
