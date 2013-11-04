package controllers.modules2;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.VisitorInfo;

public interface RawSubProcessor {

	void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor, String timeColumn, String valueColumn);

	ReadResult read();

}
