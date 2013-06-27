package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url) {
		this.meta = meta;
		this.url = url;
		String reverseParam = play.mvc.Http.Request.current().params.get("reverse");
		if("true".equalsIgnoreCase(reverseParam))
			reverse=true;

		partitionSize = meta.getTimeSeriesPartionSize();
		currentPartitionId = partition(start, partitionSize);
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
		return (time / partitionSize)*partitionSize;
	}

	@Override
	public ReadResult read() {
		AbstractCursor<Column> cursor;
		if(reverse)
			cursor = getReverseCursor();
		else
			cursor = getCursorWithResults();

		if(cursor == null)
			return new ReadResult();
		
		return translate(cursor.getCurrent());
	}

	private AbstractCursor<Column> getReverseCursor() {
		if(cursor != null && cursor.previous()) {
			return cursor;
		} else if(currentPartitionId > start)
			return null;

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
		} while(currentPartitionId > start);

		return null;	
	}

	private AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentPartitionId > end)
			return null;

		do {
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartitionId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 200);
			currentPartitionId += partitionSize;
			if(cursor.next())
				return cursor;
		} while(currentPartitionId < end);

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
