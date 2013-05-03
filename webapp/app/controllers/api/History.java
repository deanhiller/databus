package controllers.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.SecureTable;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;

import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.ApiMetaRequest;
import controllers.ApiPostDataPointsImpl;
import controllers.SecurityUtil;

public class History extends Controller {

	private static final Logger log = LoggerFactory.getLogger(ApiMetaRequest.class);
	
	private static final ObjectMapper mapper = new ObjectMapper();

	public static void fetchHistory(String tableName, String rowKey) throws JsonGenerationException, JsonMappingException, IOException {
		NoSqlTypedSession typedSession = NoSql.em().getTypedSession();
		SecureTable table = SecurityUtil.checkSingleTable(tableName);
		DboTableMeta tableMeta = table.getTableMeta();
		
		DboColumnIdMeta idMeta = tableMeta.getIdColumnMeta();
		Object primaryKeey = idMeta.convertStringToType(rowKey);
		TypedRow row = typedSession.find(tableName, primaryKeey);

		if(row == null) {
			badRequest("Row with pk="+rowKey+" does not exist in table="+tableName);
		}

		Map<String, Object> allJson = new HashMap<String, Object>();
		Map<String, Object> currentValues = new HashMap<String, Object>();
		currentValues.put("_tableName", tableName);
		currentValues.put(idMeta.getColumnName(), rowKey);

		List<Object> historyEntries = new ArrayList<Object>();
		allJson.put("current", currentValues);
		allJson.put("history", historyEntries);

		if (log.isInfoEnabled())
			log.info("process history");
		Collection<TypedColumn> columns = row.getColumnsAsColl();
		processHistory(columns, historyEntries, tableMeta);

		for(DboColumnMeta m : tableMeta.getAllColumns()) {
			TypedColumn col = row.getColumn(m.getColumnName());
			String colVal = col.getValueAsString();
			currentValues.put(m.getColumnName(), colVal);
		}

		String jsonString = mapper.writeValueAsString(allJson);
		String param = request.params.get("callback");
		if(param != null) {
			response.contentType = "text/javascript";
			jsonString = param + "(" + jsonString + ");";
		}
		renderJSON(jsonString);
	}

	private static void processHistory(Collection<TypedColumn> columns, List<Object> historyEntries, DboTableMeta tableMeta) {
		Map<String, Object> historyValues = new HashMap<String, Object>();
		List<Long> times = new ArrayList<Long>();
		String prefix = ApiPostDataPointsImpl.HISTORY_PREFIX;
		byte[] prefixBytes = StandardConverters.convertToBytes(prefix);
		if (log.isInfoEnabled())
			log.info("sizecolumns="+columns.size());
		for(TypedColumn c : columns) {
			ColumnName colName = DocumentUtil.parseName(prefixBytes, c.getNameRaw());
			if(colName != null) {
				if (log.isInfoEnabled())
					log.info("colName="+colName.getName()+" coltime="+colName.getTime());
				addToJson(colName, historyValues, times, c, tableMeta);
			}
		}
		
		Collections.sort(times);
		for(Long time : times) {
			Object jsonObj = historyValues.get(""+time);
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put(""+time, jsonObj);
			historyEntries.add(entry);
		}
		
		if (log.isInfoEnabled())
			log.info("result list="+historyEntries+" map="+historyValues);
	}

	private static void addToJson(ColumnName colName,
			Map<String, Object> historyValues, List<Long> times, TypedColumn c,
			DboTableMeta tableMeta) {
		String columnName = colName.getName();
		String time = ""+colName.getTime();
		if(!times.contains(colName.getTime()))
			times.add(colName.getTime());
		String columnValue = null;
		if("_state".equals(colName.getName())) {
			StateDocument doc = DocumentUtil.parseDocument(c.getValueRaw());
			putValue(historyValues, "_user", time, doc.getUser());
			putValue(historyValues, "_description", time, doc.getDescription());
			putValue(historyValues, "_state", time, doc.getState());
		} else {
			DboColumnMeta colMeta = tableMeta.getColumnMeta(columnName);
			Object obj = colMeta.convertFromStorage2(c.getValueRaw());
			columnValue = colMeta.convertTypeToString(obj);
			putValue(historyValues, columnName, time, columnValue);
		}
	}

	private static void putValue(Map<String, Object> historyValues,
			String columnName, String time, String columnValue) {
		Map<String, Object> historyEntry = (Map<String, Object>) historyValues.get(time);
		if(historyEntry == null) {
			historyEntry = new HashMap<String, Object>();
			historyValues.put(time, historyEntry);
		}

		historyEntry.put(columnName, columnValue);
	}
	
}
