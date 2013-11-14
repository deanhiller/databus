package controllers.modules2;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.RowMeta;

public interface RawSubProcessor {

	void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor, RowMeta rowMeta);

	ReadResult read();

}
