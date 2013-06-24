package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class RawTimeSeriesProcessor implements RawSubProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesProcessor.class);

	private DboTableMeta meta;
	private Long currentPartitionId;
	private byte[] endBytes;
	private byte[] startBytes;
	private Long end;
	private AbstractCursor<Column> cursor;
	private Integer partitionSize;
	private DboColumnMeta colMeta;

	private String url;

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url) {
		this.meta = meta;
		this.url = url;
		partitionSize = meta.getTimeSeriesPartionSize();
		currentPartitionId = partition(start, partitionSize);
		this.startBytes = meta.getIdColumnMeta().convertToStorage2(start);
		this.endBytes = meta.getIdColumnMeta().convertToStorage2(end);
		this.end = end;
		colMeta = meta.getAllColumns().iterator().next();
		if (log.isInfoEnabled())
			log.info("Setting up for reading partitions, partId="+currentPartitionId+" start="+start);
	}

	private Long partition(Long time, long partitionSize) {
		if(time == null)
			return null;
		return (time / partitionSize)*partitionSize;
	}

	@Override
	public ReadResult read() {
		AbstractCursor<Column> cursor = getCursorWithResults();
		if(cursor == null)
			return new ReadResult();
		
		return translate(cursor.getCurrent());
	}

	private AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentPartitionId > end)
			return null;

		do {
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] key = meta.getIdColumnMeta().convertToStorage2(currentPartitionId);
			cursor = raw.columnSlice(meta, key, startBytes, endBytes, 200);

			currentPartitionId += partitionSize;
		} while(!cursor.next() && currentPartitionId < end);

		return cursor;
	}

	private ReadResult translate(Column current) {
		byte[] name = current.getName();
		byte[] value = current.getValue();
		Object time = meta.getIdColumnMeta().convertFromStorage2(name);
		Object val = colMeta.convertFromStorage2(value);
		TSRelational tv = new TSRelational();
		tv.put("time", time);
		tv.put(colMeta.getColumnName(), val);
		return new ReadResult(url, tv);
	}

}
