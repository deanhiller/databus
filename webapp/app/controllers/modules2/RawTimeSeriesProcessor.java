package controllers.modules2;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.modules2.framework.ReadResult;

public class RawTimeSeriesProcessor implements RawSubProcessor {

	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url) {
	}

	@Override
	public ReadResult read() {
		return null;
	}

}
