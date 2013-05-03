package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

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
