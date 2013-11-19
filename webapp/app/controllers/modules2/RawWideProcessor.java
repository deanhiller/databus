package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.DataTypeEnum;
import models.SecureTable;
import models.message.ChartVarMeta;
import models.message.StreamModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Validation;
import play.mvc.Scope.Params;
import play.mvc.results.Forbidden;
import play.mvc.results.NotFound;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.SecurityUtil;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.DatabusBadRequest;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.RowMeta;

public class RawWideProcessor extends EndOfChain implements PullProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawWideProcessor.class);

	private RawSubProcessor subprocessor;
	private RowMeta rowMeta;
	
	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new LocalMetaInformation(parameterMeta);
	private static String NAME_IN_JAVASCRIPT = "table";
	public static String COL_NAME = "columnName";

	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Table");
		meta1.setNameInJavascript(NAME_IN_JAVASCRIPT);
		meta1.setRequired(true);
		meta1.setHelp("The table that we read data from");
		parameterMeta.put(meta1.getNameInJavascript(), meta1);
		
		metaInfo.setDescription("This module reads the raw data from wide time series tables(time series relational) and feeds it to the next module");
	}

	private static class LocalMetaInformation extends MetaInformation {

		public LocalMetaInformation(Map<String, ChartVarMeta> parameterMeta) {
			super(parameterMeta, NumChildren.NONE, false, "Wide Time Series");
		}

		@Override
		protected void validateMore(Validation validation,
				Map<String, String> variableValues) {
			super.validateMore(validation, variableValues);
			String table = variableValues.get(NAME_IN_JAVASCRIPT);
			try {
				SecureTable secTable = SecurityUtil.checkSingleTable(table);
				if(secTable == null)
					validation.addError(NAME_IN_JAVASCRIPT, "This table does not exist");
				else if(secTable.getTypeOfData() != DataTypeEnum.RELATIONAL_TIME_SERIES
						&& secTable.getTypeOfData() != DataTypeEnum.TIME_SERIES)
					validation.addError(NAME_IN_JAVASCRIPT, "This table is not a relational time series table nor a time series table");
			} catch(Forbidden e) {
				validation.addError(NAME_IN_JAVASCRIPT, "You don't have access to this table");
			} catch(NotFound e) {
				validation.addError(NAME_IN_JAVASCRIPT, "This table does not exist");
			}
		}
	}

	public RawWideProcessor() {
	}

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup child, boolean alreadyAdded) {
		return null;
	}
	
	@Override
	public MetaInformation getGuiMeta() {
		//this is per class(ie. this method could be static really except that we use polymorphism to call it on an unknown ProcessorSetup.java(superclass)
		return metaInfo;
	}

	@Override
	public RowMeta getRowMeta() {
		//this is per instance
		return rowMeta;
	}
	
	@Override
	public void createTree(ProcessorSetup parent, StreamModule info, VisitorInfo visitor) {
		this.parent = parent;
		Map<String, String> options = info.getParams();

		String table = options.get(NAME_IN_JAVASCRIPT);

		SecureTable sdiTable = SecurityUtil.checkSingleTable(table);
		
		if(sdiTable == null)
			throw new DatabusBadRequest("table="+table+" does not exist");

		timeColumn = sdiTable.getPrimaryKey().getColumnName();
		List<String> names = new ArrayList<String>();
		Set<String> keySet = sdiTable.getNameToField().keySet();
		for(String n : keySet) {
			names.add(n);
		}
		
		rowMeta = new RowMeta(timeColumn, names);
		
		DboTableMeta meta = sdiTable.getTableMeta();
		
		Long start = parseParam("start");
		Long end = parseParam("end");

		if(visitor.isReversed())
			subprocessor = new RawTimeSeriesReversedProcessor();
		else
			subprocessor = new RawTimeSeriesProcessor();
		
		subprocessor.init(meta, start, end, null, visitor, rowMeta);
	}

	private Long parseParam(String name) {
		Params params = Params.current();
		if(params == null)
			return null;
		
		String timeVal = params.get(name);
		if(timeVal == null)
			return null;
		return Long.parseLong(timeVal);
	}
	
	@Override
	public void start(VisitorInfo visitor) {
		//nothing to do, engine is the one who starts reading from us
	}

	@Override
	public ReadResult read() {
		ReadResult r = subprocessor.read();
		return r;
	}

	@Override
	public Direction getSinkDirection() {
		return Direction.NONE;
	}

	@Override
	public Direction getSourceDirection() {
		return Direction.PULL;
	}

	@Override
	public void startEngine() {
		throw new UnsupportedOperationException("bug, never needs to be called");
	}

}
