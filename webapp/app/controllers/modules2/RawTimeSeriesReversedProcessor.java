package controllers.modules2;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.impl.RawTimeSeriesImpl;
import controllers.impl.RawTimeSeriesReversedImpl;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.RowMeta;

public class RawTimeSeriesReversedProcessor extends RawTimeSeriesProcessor
		implements RawSubProcessor {

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor, RowMeta rowMeta) {
		this.url = url;
		impl = new RawTimeSeriesReversedImpl();
		impl.init(meta, start, end, rowMeta, visitor.getMgr());
	}
}
