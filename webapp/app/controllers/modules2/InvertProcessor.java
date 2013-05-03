package controllers.modules2;

import java.math.BigDecimal;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class InvertProcessor extends PushOrPullProcessor {

	@Override
	protected TSRelational modifyRow(TSRelational row) {
		BigDecimal val = getValueEvenIfNull(row);
		if(val != null)
			setValue(row, val.negate());
		
		return row;
	}

}
