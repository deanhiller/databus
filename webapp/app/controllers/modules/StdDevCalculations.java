package controllers.modules;

import gov.nrel.util.TimeValue;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import controllers.modules.util.JsonRow;

public class StdDevCalculations {

	private SummaryStatistics summaryStats;
	private DescriptiveStatistics descStats;
	private StatisticalSummary backingSummary;
	
	public StdDevCalculations(int rowCount) {
		if (rowCount < 1) {
			summaryStats = new SummaryStatistics();
			backingSummary = summaryStats;
		}
		else {
			descStats = new DescriptiveStatistics(rowCount);
			backingSummary = descStats;
		}
	}

	public StatisticalSummary processSingleChunk(List<TimeValue> rows) {
		
		for (TimeValue row:rows){
			if(row.getValue() != null) {
				addValue(row.getValue());
			}
		}	
		return backingSummary;
	}
	
	private void addValue(BigDecimal val) {
		if (summaryStats != null)
			summaryStats.addValue(val.doubleValue());
		else
			descStats.addValue(val.doubleValue());
	}
	

	
}
