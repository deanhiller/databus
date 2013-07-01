package controllers.modules2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.SecureResourceGroupXref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Util;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.exc.ParseException;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.orm.api.z8spi.meta.ViewInfo;
import com.alvazan.play.NoSql;

import controllers.ApiGetData;
import controllers.SecurityUtil;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class SqlPullProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(SqlPullProcessor.class);
	private Iterator<List<TypedRow>> iterator;

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup child, boolean alreadyAdded) {
		return null;
	}

	@Override
	public void start(VisitorInfo visitor) {
		List<String> parameters = params.getParams();
		if(parameters.size() == 0)
			throw new BadRequest("rawdata module requires a column family name");
		String sql = parameters.get(0);
		//If this is the first time in here, it is a new request
		String correctSql = sql.replace("+", " ");
		QueryResult result = securityChecks(correctSql);
		Iterable<List<TypedRow>> cursor = result.getAllViewsIter();
		iterator = cursor.iterator();
	}

	@Util
	private static QueryResult securityChecks(String sql) {
		try {
			return securityChecksImpl(sql);
		} catch(ParseException e) {
			Throwable childExc = e.getCause();
			if (log.isInfoEnabled())
				log.info("parse exception", e);
			throw new BadRequest("Your sql is invalid.  reason="+childExc.getMessage());
		}
	}

	private static QueryResult securityChecksImpl(String sql) {		
		NoSqlEntityManager em = NoSql.em();
		NoSqlTypedSession s = em.getTypedSession();
		
		QueryResult result = s.createQueryCursor(sql, ApiGetData.BATCH_SIZE);

		Map<String, SecureResourceGroupXref> schemaIds = SecurityUtil.groupSecurityCheck();
		List<ViewInfo> views = result.getViews();
		
		//check access for every table in every group
		for(ViewInfo view : views) {
			String cf = view.getTableMeta().getColumnFamily();
			SecurityUtil.checkTable(schemaIds, cf);
		}
		return result;
	}

	@Override
	public ReadResult read() {
		if(!iterator.hasNext())
			return new ReadResult();
		List<TypedRow> typedRows = iterator.next();
		ReadResult res = translate(typedRows);
		return res;
	}

	private ReadResult translate(List<TypedRow> typedRows) {
		TSRelational tv = new TSRelational(timeColumn, valueColumn);
		for(int i = 0; i < typedRows.size(); i++) {
			TypedRow row = typedRows.get(i);
			DboTableMeta meta = row.getView().getTableMeta();
			translate(meta, row, tv);
		}
		
		return new ReadResult(null, tv);
	}
	
	public static void translate(DboTableMeta meta, TypedRow row, TSRelational tv) {
		DboColumnMeta idMeta = meta.getIdColumnMeta();
		if(row.getRowKey() == null)
			return;
		Object val = row.getRowKey();
		String colNameVal = idMeta.getColumnName();
		tv.put(colNameVal, val);
		
		for(DboColumnMeta colMeta : meta.getAllColumns()) {
			createJson(row, colMeta, tv);
		}
	}

	private static void createJson(TypedRow row, DboColumnMeta colMeta, TSRelational tv) {
		String colName = colMeta.getColumnName();
		TypedColumn column = row.getColumn(colName);
		if(column != null) {
			Object val = column.getValue();
			tv.put(colName, val);
		}
	}

	// TSRelational row = new TSRelational();
	// setValue(row, val);
	// setTime(row, val);
	// new ReadResult(getUrl(),row);
}
