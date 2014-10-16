package controllers.modules2;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.PlayPlugin;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.impl.RawTimeSeriesImpl;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.RowMeta;

public class RawTimeSeriesProcessor extends PlayPlugin implements RawSubProcessor {
//public class RawTimeSeriesProcessor implements RawSubProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawTimeSeriesProcessor.class);

	protected RawTimeSeriesImpl impl;
	protected String url;


	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor, RowMeta rowMeta) {
		this.url = url;
		impl = new RawTimeSeriesImpl();
		impl.init(meta, start, end, rowMeta, visitor.getMgr());
	}

	
	@Override
	public ReadResult read() {
		TSRelational ts = impl.nextResult();
		if (ts == null)
			return new ReadResult();
		return new ReadResult(url, ts);
	}

}
