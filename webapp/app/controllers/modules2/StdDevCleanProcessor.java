package controllers.modules2;

import gov.nrel.util.TimeValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import models.KeyToTableName;
import models.SecureTable;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;
import play.mvc.results.Unauthorized;
import controllers.modules.SplinesBigDecBasic;
import controllers.modules.SplinesBigDecLimitDerivative;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class StdDevCleanProcessor extends PullProcessorAbstract {

	private ArrayList<ReadResult> theRows = new ArrayList<ReadResult>();
	private int rowCount;
	private double factor;
	private StdDevCalculations calculations;
	
	@Override
	protected int getNumParams() {
		return 2;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor) {
		String newPath = super.init(path, nextInChain, visitor);
		String msg = "module url /stddevV1/{rowCount}/{factor} must be passed an integer for rowCount and was not passed that";
		rowCount = parseInteger(params.getParams().get(0), msg);
		if(rowCount < 0)
			throw new BadRequest("rowCount cannot be less than 0.  your rowCount="+rowCount+" if rowcount is 0, we do a running stddev");
		calculations = new StdDevCalculations(rowCount);
		String strVal = params.getParams().get(1);
		factor = new BigDecimal(strVal).doubleValue();
		return newPath;
	}
	
	@Override
	public ReadResult read() {
		while(true) {
			ReadResult res = readImpl();
			if(res != null)
				return res;
		}
	}

	private ReadResult readImpl() {
		if(theRows.size() > 0) {
			return nextValidValue();
		}

		if(rowCount > 0) {
			calculations = new StdDevCalculations(rowCount);

			for(int i = 0; i < rowCount; i++) {
				ReadResult result = addNextValToList();
				if(result.isEndOfStream())
					break;
			}
		} else {
			addNextValToList();
		}

		return nextValidValue();
	}

	private ReadResult addNextValToList() {
		ReadResult result = getChild().read();
		theRows.add(result);
		TSRelational row2 = result.getRow();
		if(row2 != null) {
			BigDecimal val = getValueEvenIfNull(row2);
			if(val != null) {
				calculations.addValue(val);
			}
		}
		return result;
	}

	private ReadResult nextValidValue() {
		StatisticalSummary summary = calculations.getSummary();
		double mean = summary.getMean();
		double std = summary.getStandardDeviation();
		double allowedVariance = std*factor;
		double min = mean-allowedVariance;
		double max = mean+allowedVariance;
		
		while(theRows.size() > 0) {
			ReadResult res = theRows.remove(0);
			TSRelational row = res.getRow();
			if(row == null)
				return res;
			BigDecimal val = getValueEvenIfNull(row);
			if(val == null)
				return res;
			else if(val.doubleValue() >= min && val.doubleValue() <= max)
				return res;
		}

		//not enough data, exit to while loop so we read more again
		return null;
	}

	class StdDevCalculations {
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
		
		private void addValue(BigDecimal val) {
			if (summaryStats != null)
				summaryStats.addValue(val.doubleValue());
			else
				descStats.addValue(val.doubleValue());
		}
		
		public StatisticalSummary getSummary() {
			return backingSummary;
		}
	}
}
