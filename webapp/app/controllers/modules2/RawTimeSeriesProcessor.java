package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;

public class RawTimeSeriesProcessor implements RawSubProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesProcessor.class);

	protected DboTableMeta meta;
	protected byte[] endBytes;
	protected byte[] startBytes;
	protected Long start;
	protected Long end;
	protected AbstractCursor<Column> cursor;
	protected Long partitionSize;
	private DboColumnMeta colMeta;
	private String url;
	protected int currentIndex;
	protected List<Long> existingPartitions = new ArrayList<Long>();

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor) {
		this.meta = meta;
		this.url = url;
		
		this.start = start;
		this.end = end;
		loadPartitions(meta);
		partitionSize = meta.getTimeSeriesPartionSize();
		currentIndex = partition(partitionSize);

		if(start != null)
			this.startBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(start+""));
		if(end != null)
			this.endBytes = meta.getIdColumnMeta().convertToStorage2(new BigInteger(end+""));

		colMeta = meta.getAllColumns().iterator().next();
		if (log.isInfoEnabled())
			log.info("Setting up for reading partitions, partId="+currentIndex+" partitions="+existingPartitions+" start="+start);
	}

	private void loadPartitions(DboTableMeta meta2) {
		DboTableMeta tableMeta = NoSql.em().find(DboTableMeta.class, "partitions");
		NoSqlSession session = NoSql.em().getSession();
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

	@Override
	public ReadResult read() {
		AbstractCursor<Column> cursor = getCursorWithResults();
		if(cursor == null)
			return new ReadResult(); //no more data to read

		return translate(cursor.getCurrent());
	}

	protected AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.next()) {
			return cursor;
		} else if(currentIndex >= existingPartitions.size())
			return null;

		int count = 0;
		do {
			Long currentPartId = existingPartitions.get(currentIndex);
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 500, BigInteger.class);
			currentIndex++;
			if(cursor.next())
				return cursor;
			count++;

		} while(!hasReachedEnd(end));

		//reached end and no data found
		return null;
	}

	private boolean hasReachedEnd(Long end) {
		if(currentIndex >= existingPartitions.size())
			return true;
		Long partId = existingPartitions.get(currentIndex);
		if(end < partId)
			return true;
		return false;
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
