package controllers.modules2;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.modules2.framework.ReadResult;

public interface RawSubProcessor {

	void init(DboTableMeta meta, Long start, Long end, String url);

	ReadResult read();

}
