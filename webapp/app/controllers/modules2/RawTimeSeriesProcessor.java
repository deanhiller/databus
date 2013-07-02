package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.util.BigInt;

import gov.nrel.util.Utility;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;

public class RawTimeSeriesProcessor implements RawSubProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesProcessor.class);

	private DboTableMeta meta;
	private Long currentPartitionId;
	private byte[] endBytes;
	private byte[] startBytes;
	private Long start;
	private Long end;
	private AbstractCursor<Column> cursor;
	private Long partitionSize;
	private DboColumnMeta colMeta;
	private boolean reverse = false;
	private String url;

	private Long startPartition;

	private boolean noData;

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor) {
		this.meta = meta;
		this.url = url;
		this.reverse = visitor.isReversed();

		partitionSize = meta.getTimeSeriesPartionSize();
		currentPartitionId = partition(start, partitionSize);
		if(reverse)
			currentPartitionId = partition(end, partitionSize);

		startPartition = currentPartitionId;
		this.startBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(start+""));
		this.endBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(end+""));
		this.start = start;
		this.end = end;
		colMeta = meta.getAllColumns().iterator().next();
		if (log.isInfoEnabled())
			log.info("Setting up for reading partitions, partId="+currentPartitionId+" start="+start);
	}

	private Long partition(Long time, long partitionSize) {
		if(time == null)
			return null;
		long partitionId = (time / partitionSize)*partitionSize;
		if(partitionId < 0) {
			//if partitionId is less than 0, it incorrectly ends up in the higher partition -20/50*50 = 0 and 20/50*50=0 when -20/50*50 needs to be -50 partitionId
			if(Long.MIN_VALUE+partitionSize >= partitionId)
				partitionId = Long.MIN_VALUE;
			else
				partitionId -= partitionSize; //subtract one partition size off of the id
		}
		return partitionId;
	}

	@Override
	public ReadResult read() {
		AbstractCursor<Column> cursor;
		try {
			if(noData)
				return new ReadResult();
			else if(reverse)
				cursor = getReverseCursor();
			else
				cursor = getCursorWithResults();
		} catch(NoDataException e) {
			String time1 = convert(startPartition);
			String time2 = convert(currentPartitionId);
			noData = true;
			return new ReadResult("", "We scanned partitions from time="+time1+" to time="+time2+" and found no data so are exiting.  perhaps add a start time and end time to your url");
		}

		if(cursor == null)
			return new ReadResult();

		return translate(cursor.getCurrent());
	}

	private String convert(long startPartition2) {
		DateTime dt = new DateTime(startPartition2);
		return dt+"";
	}

	private AbstractCursor<Column> getReverseCursor() {
		if(cursor != null && cursor.previous()) {
			return cursor;
		} else if(currentPartitionId < (start-partitionSize))
			return null;

		int count = 0;
		do {
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartitionId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 200);
			cursor.afterLast();
			currentPartitionId -= partitionSize;
			if(cursor.previous())
				return cursor;
			count++;
		} while(currentPartitionId > start && count < 20);

		if(count >= 20)
			throw new NoDataException("no data in 20 partitions found...not continuing");

		return null;	
	}

	private AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentPartitionId > end)
			return null;

		int count = 0;
		do {
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartitionId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 200);
			currentPartitionId += partitionSize;
			if(cursor.next())
				return cursor;
			count++;
		} while(currentPartitionId < end && count < 20);

		if(count >= 20)
			throw new NoDataException("no data in 20 partitions found...not continuing");
		return null;
	}

	private ReadResult translate(Column current) {
		byte[] name = current.getName();
		byte[] value = current.getValue();
		Object time = meta.getIdColumnMeta().convertFromStorage2(name);
		Object val = colMeta.convertFromStorage2(value);
		//TODO:  parameterize timeColumn and valueColumn from options
		TSRelational tv = new TSRelational("time", "value");
		tv.put("time", time);
		tv.put(colMeta.getColumnName(), val);
		return new ReadResult(url, tv);
	}

}
