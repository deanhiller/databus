package controllers.modules2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import models.message.ChartVarMeta;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class InvertProcessor extends PushOrPullProcessor {

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, NumChildren.ONE, true, "Invert", "Math Operations");

	static {
		metaInfo.setDescription("This module inverts the value of the child processor.");
	}

	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational row) {
		BigDecimal val = getValueEvenIfNull(row);
		if(val != null)
			setValue(row, val.negate());
		
		return row;
	}

}
