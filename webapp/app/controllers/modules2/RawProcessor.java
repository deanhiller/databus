package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class RawProcessor extends EndOfChain implements PullProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawProcessor.class);

	private BigDecimal maxInt = new BigDecimal(Integer.MAX_VALUE);
	private RawSubProcessor subprocessor;
	private boolean skipSecurity;

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new LocalMetaInformation(parameterMeta);
	private static String NAME_IN_JAVASCRIPT = "table";
	private static String COL_NAME = "columnName";

	static {
		ChartVarMeta meta1 = new ChartVarMeta();
		meta1.setLabel("Table");
		meta1.setNameInJavascript(NAME_IN_JAVASCRIPT);
		meta1.setRequired(true);
		meta1.setHelp("The table that we read data from");
		parameterMeta.put(meta1.getNameInJavascript(), meta1);

		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Output Column");
		meta.setNameInJavascript(COL_NAME);
		meta.setRequired(true);
		meta.setHelp("The name of the column to be used in output to the next module up");
		parameterMeta.put(meta.getNameInJavascript(), meta);
		
		metaInfo.setDescription("This module reads the raw data from the database and feeds it to the next module");
	}

	private static class LocalMetaInformation extends MetaInformation {

		public LocalMetaInformation(Map<String, ChartVarMeta> parameterMeta) {
			super(parameterMeta, NumChildren.NONE, false, "Table Data Source");
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
			} catch(Forbidden e) {
				validation.addError(NAME_IN_JAVASCRIPT, "You don't have access to this table");
			} catch(NotFound e) {
				validation.addError(NAME_IN_JAVASCRIPT, "This table does not exist");
			}
		}
	}

	public RawProcessor() {
	}
	public RawProcessor(boolean skipSecurity) {
		this.skipSecurity = skipSecurity;
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
		return metaInfo;
	}

	@Override
	public void createTree(ProcessorSetup parent, StreamModule info, VisitorInfo visitor) {
		this.parent = parent;
		Map<String, String> options = info.getParams();

		String table = options.get(NAME_IN_JAVASCRIPT);
		valueColumn = options.get(COL_NAME);
		timeColumn = "time";
		
		SecureTable sdiTable = null;
		if(skipSecurity) {
			sdiTable = SecureTable.findByName(visitor.getMgr(), table);
		} else
			sdiTable = SecurityUtil.checkSingleTable(table);
		
		if(sdiTable == null)
			throw new DatabusBadRequest("table="+table+" does not exist");

		DboTableMeta meta = sdiTable.getTableMeta();
		
		Long start = parseParam("start");
		Long end = parseParam("stop");

		if(meta.isTimeSeries()) {
			if(visitor.isReversed())
				subprocessor = new RawTimeSeriesReversedProcessor();
			else
				subprocessor = new RawTimeSeriesProcessor();
		} else
			subprocessor = new RawStreamProcessor();
		
		subprocessor.init(meta, start, end, null, visitor, timeColumn, valueColumn);
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, Map<String, String> options) {
		String res = super.init(path, nextInChain, visitor, options);
		List<String> parameters = params.getParams();
		if(parameters.size() == 0)
			throw new DatabusBadRequest("rawdata module requires a column family name");
		String colFamily = parameters.get(0);
		Long start = params.getOriginalStart();
		Long end = params.getOriginalEnd();

		if(start == null)
			start = parseParam("start");
		if(end == null)
			end = parseParam("stop");
		
		SecureTable sdiTable = null;
		if(skipSecurity) {
			sdiTable = SecureTable.findByName(visitor.getMgr(), colFamily);
		} else
			sdiTable = SecurityUtil.checkSingleTable(colFamily);
		
		if(sdiTable == null)
			throw new DatabusBadRequest("table="+colFamily+" does not exist");

		DboTableMeta meta = sdiTable.getTableMeta();
		
		if(meta.isTimeSeries()) {
			if(visitor.isReversed())
				subprocessor = new RawTimeSeriesReversedProcessor();
			else
				subprocessor = new RawTimeSeriesProcessor();
		} else
			subprocessor = new RawStreamProcessor();
		
		subprocessor.init(meta, start, end, params.getPreviousPath(), visitor, "time", "value");
		return res;
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
		TSRelational tv = r.getRow();
		if(tv != null)
			modifyForMaxIntBug(tv);
		return r;
	}

	private void modifyForMaxIntBug(TSRelational tv) {
		//because of a single client that puts in Integer.MAX_INT, we need to modify all Integer.MAX_INT to return null
		BigDecimal dec = getValueEvenIfNull(tv);
		if(maxInt.equals(dec)) {
			setValue(tv, null);
		}
	}
	
	@Override
	public String getUrl() {
		return params.getPreviousPath();
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
