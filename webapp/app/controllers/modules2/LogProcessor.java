package controllers.modules2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import models.PermissionType;
import models.SecureTable;
import play.mvc.Http.Request;
import play.mvc.results.BadRequest;
import play.mvc.results.Unauthorized;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.SecurityUtil;
import controllers.api.ApiPostDataPointsImpl;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class LogProcessor extends PullProcessorAbstract {

	private static final int BATCH_SIZE = 200;
	private DboTableMeta tableMeta;
	private ArrayList<ReadResult> theRows = new ArrayList<ReadResult>();
	
	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		// param 1: Type: String
		String tableName = params.getParams().get(0);
		NoSqlEntityManager s = NoSql.em();
		Request request = Request.current();
		
		PermissionType permission = SecurityUtil.checkSingleTable2(tableName);
		if(PermissionType.ADMIN.isHigherRoleThan(permission))
			throw new Unauthorized("You are not authorized as an admin of database for table="+tableName);

		SecureTable sdiTable = SecureTable.findByName(s, tableName);
		if(!sdiTable.isForLogging())
			throw new BadRequest("Table="+tableName+" was not registered as a table for logging to");
		tableMeta = sdiTable.getTableMeta();
		
		//This next piece is just so we work in any thread as it forces a load from the database
		tableMeta.initCaches();
		Collection<DboColumnMeta> columns = tableMeta.getAllColumns();
		for(DboColumnMeta m : columns) {
			//force load from noSQL store right now...
			m.getColumnName();
		}
		
		
		return newPath;
	}
	
	@Override
	public ReadResult read() {
		if(theRows.size() > 0) {
			return theRows.remove(0);
		}

		//let's read 200 and write 200 so we get the speed of parallizing the writes/reads
		NoSqlTypedSession typedSession = NoSql.em().getTypedSession();
		for(int i = 0; i < BATCH_SIZE; i++) {
			ReadResult result = getChild().read();
			if(result == null)
				break;

			theRows.add(result);
			TSRelational row2 = result.getRow();
			if(row2 != null) {
				saveData(typedSession, row2);
			}
			if(result.isEndOfStream())
				break;
		}

		//NOW, let's go through and save all 200 to the database
		typedSession.flush();

		if(theRows.size() > 0)
			return theRows.remove(0);
		return null;// will continue when lower layers have more data
	}

	private void saveData(NoSqlTypedSession typedSession, TSRelational row2) {
		if(tableMeta.isTimeSeries())
			saveTimeSeries(typedSession, row2);
		else {
			saveRelational(typedSession, row2);
		}
	}

	private void saveTimeSeries(NoSqlTypedSession typedSession, TSRelational row2) {
		String idName = tableMeta.getIdColumnMeta().getColumnName();
		Object idVal = row2.get(idName);
		
		DboColumnMeta singleCol = tableMeta.getAllColumns().iterator().next();
		Object val = row2.get(singleCol.getColumnName());
		
		ApiPostDataPointsImpl.postTimeSeriesImpl(NoSql.em(), tableMeta, idVal, val, false);
	}

	private void saveRelational(NoSqlTypedSession typedSession,
			TSRelational row2) {
		TypedRow row = typedSession.createTypedRow(tableMeta.getColumnFamily());

		String idName = tableMeta.getIdColumnMeta().getColumnName();
		Object idVal = row2.get(idName);
		row.setRowKey(idVal);

		for(DboColumnMeta m : tableMeta.getAllColumns()) {
			Object val = row2.get(m.getColumnName());
			row.addColumn(m.getColumnName(), val);
		}
		
		typedSession.put(tableMeta.getColumnFamily(), row);
	}

}
