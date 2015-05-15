package controllers;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;
import controllers.impl.RawTimeSeriesImpl;
import controllers.modules2.framework.procs.RowMeta;
import gov.nrel.util.SearchUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import models.PermissionType;
import models.SdiColumn;
import models.SecureTable;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;

public class ApiMetaRequest extends Controller {

	private static final Logger log = LoggerFactory.getLogger(ApiMetaRequest.class);
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static void getMeta(String table) throws JsonGenerationException, JsonMappingException, IOException {
		SecureTable sdiTable = SecurityUtil.checkSingleTable(table);
		DboTableMeta tableMeta = sdiTable.getTableMeta();
		
		RegisterMessage msg = new RegisterMessage();
		DatasetType t;
		switch (sdiTable.getTypeOfData()) {
		case RELATIONAL:
			t = DatasetType.RELATIONAL_TABLE;
			break;
		case TIME_SERIES:
			t = DatasetType.STREAM;
			break;
		default:
			if (log.isWarnEnabled())
        		log.warn("BUG, they requested a data type we have in database but can't translate="+sdiTable.getTypeOfData());
			error("Bug on our side for table of type="+sdiTable.getTypeOfData());
			return;
		}
		
		msg.setDatasetType(t);
		msg.setModelName(table);
		msg.setIsSearchable(sdiTable.isSearchable());
		
		List<DatasetColumnModel> columns = formColumns(tableMeta, sdiTable);
		msg.setColumns(columns );

		String jsonString = mapper.writeValueAsString(msg);
		String param = request.params.get("callback");
		if(param != null) {
			response.contentType = "text/javascript";
			jsonString = param + "(" + jsonString + ");";
		}
		renderJSON(jsonString);
	}
	
	public static void deleteRange(String table, String start, String end) {
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		PermissionType permission = SecurityUtil.checkSingleTable2(table);
		if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
			unauthorized("You don't have permission to modify the data in this table");
		RawTimeSeriesImpl impl = new RawTimeSeriesImpl();
		
		String timeColumn = targetTable.getPrimaryKey().getColumnName();
		List<String> names = new ArrayList<String>();
		Set<String> keySet = targetTable.getNameToField().keySet();
		for(String n : keySet) {
			names.add(n);
		}
		
		RowMeta rowMeta = new RowMeta(timeColumn, names);
		NoSqlEntityManager mgr = NoSql.em();
		
		impl.init(targetTable.getTableMeta(), Long.valueOf(start), Long.valueOf(end), rowMeta, mgr);
		impl.deleteRange();
	}
	
	public static void deleteTable(String table) {
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		PermissionType permission = SecurityUtil.checkSingleTable2(table);
		if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
			unauthorized("You don't have permission to modify the data in this table");
		RawTimeSeriesImpl impl = new RawTimeSeriesImpl();
		
		String timeColumn = targetTable.getPrimaryKey().getColumnName();
		List<String> names = new ArrayList<String>();
		Set<String> keySet = targetTable.getNameToField().keySet();
		for(String n : keySet) {
			names.add(n);
		}
		
		RowMeta rowMeta = new RowMeta(timeColumn, names);
		NoSqlEntityManager mgr = NoSql.em();
		
		impl.init(targetTable.getTableMeta(), null, null, rowMeta, mgr);
		impl.deleteAll();
                //delaetion of Metadata
                DboTableMeta meta = targetTable.getTableMeta();
		mgr.remove(targetTable);
                mgr.remove(meta);
		
		try {
			SearchUtils.unindexTable(targetTable);
		} catch (Exception e) {
			//TODO:  how should I signify to the requestor that the delete succeeded, but unindex failed!?
			//For now just call this a success and log the error.
			log.warn("Unindexing the table failed on table deletion of "+targetTable.getTableName(), e);
			e.printStackTrace();
		}
		
		mgr.flush();
	}

	private static List<DatasetColumnModel> formColumns(DboTableMeta tableMeta, SecureTable sdiTable) {
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		
		DboColumnIdMeta idMeta = tableMeta.getIdColumnMeta();
		SdiColumn sdiCol = sdiTable.getNameToField().get(idMeta.getColumnName());
		DatasetColumnModel model = formModel(idMeta, sdiCol, true);
		cols.add(model);
		
		for(DboColumnMeta m : tableMeta.getAllColumns()) {
			SdiColumn sdi = sdiTable.getNameToField().get(m.getColumnName());
			DatasetColumnModel mod = formModel(m, sdi, false);
			cols.add(mod);
		}
		return cols;
	}

	private static DatasetColumnModel formModel(DboColumnMeta colMeta, SdiColumn sdiCol, boolean isPrimaryKey) {
		DatasetColumnModel m = new DatasetColumnModel();
		m.setName(colMeta.getColumnName());
		m.setIsPrimaryKey(isPrimaryKey);
		m.setIsIndex(colMeta.isIndexed());
		
		String dataType = null;
		switch (colMeta.getStorageType()) {
		case STRING:
			dataType = "string";
			break;
		case BOOLEAN:
			dataType = "boolean";
			break;
		case DECIMAL:
			dataType = "bigdecimal";
			break;
		case INTEGER:
			dataType = "biginteger";
			break;
		case BYTES:
			dataType = "bytes";
			break;
		default:
			break;
		}
		
		m.setDataType(dataType);
		if(sdiCol != null)
			m.setSemanticType(sdiCol.getSemanticType());
		
		return m;
	}

}
