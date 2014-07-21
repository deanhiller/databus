package controllers.impl;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import controllers.modules2.RawTimeSeriesProcessor;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.RowMeta;

public class RawTimeSeriesReversedImpl extends RawTimeSeriesImpl {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesReversedImpl.class);

	@Override
	protected int partition(long partitionSize) {
		if(existingPartitions.size() > 0 && end == null) 
			return existingPartitions.size()-1;

		for(int i = existingPartitions.size()-1; i >= 0;i--) {
			long partId = existingPartitions.get(i);
			if(end >= partId)
				return i;
		}
		return -1;
	}
	
	@Override
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
			NoSqlTypedSession em = mgr.getTypedSession();
			NoSqlSession raw = em.getRawSession();

			byte[] rowKeyPostFix = meta.getIdColumnMeta().convertToStorage2(new BigInteger(""+currentPartId));
			byte[] rowKey = meta.getIdColumnMeta().formVirtRowKey(rowKeyPostFix);
			cursor = raw.columnSlice(meta, rowKey, startBytes, endBytes, 500, BigInteger.class);
			cursor.afterLast();
			currentIndex--;
			if(cursor.previous())
				return cursor;
		} while(!hasReachedEnd(start));

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
