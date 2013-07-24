package controllers.modules2;

import java.math.BigInteger;

import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;

public class RawTimeSeriesReversedProcessor extends RawTimeSeriesProcessor
		implements RawSubProcessor {

	protected Integer partition(long partitionSize) {
		for(int i = existingPartitions.size()-1; i >= 0;i--) {
			long partId = existingPartitions.get(i);
			if(end >= partId)
				return i;
		}
		return null;
	}

	protected AbstractCursor<Column> getCursorWithResults() {
		if(cursor != null && cursor.previous()) {
			return cursor;
		} else if(currentIndex < 0) {
			//here unlike going forwards currentPartitionId < start is valid and normal situation but
			//currentPartitionId should not be before (start-partitionSize) as we are only searching for data
			//from start and forward so this else if is different than getCursorWithResults one
			return null;
		}

		do {
			Long currentPartId = existingPartitions.get(currentIndex);
			NoSqlTypedSession em = NoSql.em().getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 500, BigInteger.class);
			cursor.afterLast();
			currentIndex--;
			if(cursor.previous())
				return cursor;
		} while(hasReachedEnd(start));

		return null;	
	}
	
	private boolean hasReachedEnd(Long end) {
		if(currentIndex < 0)
			return true;
		Long partId = existingPartitions.get(currentIndex);
		if(start > partId+partitionSize)
			return true;
		return false;
	}
}
