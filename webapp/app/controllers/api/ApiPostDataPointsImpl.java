package controllers.api;

import gov.nrel.util.SearchUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.KeyToTableName;
import models.SecureTable;
import models.message.PostTrigger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor.ToBytesTransformer;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.playorm.cron.impl.db.WebNodeDbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.results.BadRequest;
import play.mvc.results.Unauthorized;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.conv.StorageTypeEnum;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.SearchPosting;
import controllers.TableKey;
import controllers.gui.util.ExecutorsSingleton;

public class ApiPostDataPointsImpl extends PlayPlugin {
//public class ApiPostDataPointsImpl {

	private static final String STATE_COL_NAME = "_state";

	public static String HISTORY_PREFIX = "_history";
	private final static Charset charset = StandardCharsets.UTF_8;

	private static final Logger log = LoggerFactory.getLogger(ApiPostDataPointsImpl.class);
	
	private static final ObjectMapper mapper = new ObjectMapper();

	public static int postDataImpl(String json, Map<String, Object> data, String user, String password, String path) {
		//temporary version of 'upload module'.
		//eventually we will have the concept of an upload module just like our current 
		//modules that run on data retrieval.  For now, lets keep the interface the same 
		//(chain functions into the url with options in perens), but just implement this 
		//one 'module' here.
		boolean timeIsISOFormat = false;
		String timeISOFormatColumn = "time";
		String timeISOStringFormat = "";
		if (StringUtils.contains(path, "dateformatV1")) {
			timeIsISOFormat = true;
			String utcModString = StringUtils.substringBetween(path, "/dateformatV1(", ")/");
			if (StringUtils.isNotEmpty(utcModString)) {
				String[] options = StringUtils.split(utcModString, ",");
				for (String s:options) {
					String[] parts = StringUtils.split(utcModString, "=");
					//no idea why 'timeFormat' instead of 'dateFormat' was the original param name.
					//use 'dateFormat', but allow the old 'timeFormat' for backward compatibility
					if (parts.length == 2 && StringUtils.equals(parts[0].trim(), "columnName"))
						timeISOFormatColumn = parts[1];
					else if (parts.length == 2 && (StringUtils.equals(parts[0].trim(), "timeFormat") || StringUtils.equals(parts[0].trim(), "dateFormat")))
						timeISOStringFormat = parts[1];
					else
						throw new RuntimeException("The format of the dateformat module is not correct, it must be ../dateformatV1/... or .../dateformatV1(columnName=<colName>)/... or .../dateformatV1(columnName=<colName>,timeFormat=<timeFormat>)/...");

				}
			}
		}
		
		NoSqlEntityManager s = NoSql.em();
		List dataPoints = (List) data.get("_dataset");
		if(dataPoints == null) {
			//let's just wrap the single datapoint in a batch then instead...
			dataPoints = new ArrayList();
			dataPoints.add(data);
		}
		
		Collection<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();

		List<String> rowKeys = new ArrayList<String>();
		List<Map<String, String>> dataPts = new ArrayList<Map<String,String>>();
		for(Object map : dataPoints) {
			Map<String, String> row = (Map<String, String>) map;
			String tableName = (String) row.get("_tableName");
			
			if(user == null) {
				if (log.isInfoEnabled())
					log.info("user is not there, we require user and password");
				throw new Unauthorized("user must be supplied and was null in basic auth");
			}

			String rowKey = KeyToTableName.formKey(tableName, user, password);
			rowKeys.add(rowKey);
			dataPts.add(row);
		}

		//Now find all keys in one shot...
		List<KeyValue<KeyToTableName>> info = s.findAllList(KeyToTableName.class, rowKeys);
		
		Cursor<KeyValue<WebNodeDbo>> cursor = WebNodeDbo.findAllNodes(NoSql.em());
		List<WebNodeDbo> nodes = new ArrayList<WebNodeDbo>();
		int upNodeCount = 0;
		while(cursor.next()) {
			KeyValue<WebNodeDbo> kv = cursor.getCurrent();
			WebNodeDbo node = kv.getValue();
			if(node.isUp()) {
				upNodeCount++;
				nodes.add(node);
			}
		}

		Set<TableKey> keyCheck = new HashSet<TableKey>();
		for(int i = 0; i < dataPts.size(); i++) {
			KeyValue<KeyToTableName> keyValue = info.get(i);
			Map<String, String> row = dataPts.get(i);
			processData(row, solrDocs, keyValue, user, password, keyCheck, timeIsISOFormat, timeISOFormatColumn, timeISOStringFormat, nodes, upNodeCount);
		}
		
		s.flush();
		if (SearchUtils.solrIsActivated())
			SearchPosting.saveSolr(json, solrDocs, null);

		return dataPoints.size();
	}


	private static void processData(Map<String, String> jsonRow,
			Collection<SolrInputDocument> solrDocs, KeyValue<KeyToTableName> keyValue, String user, String password, Set<TableKey> keyCheck,
			boolean timeIsISOFormat, String timeISOFormatColumn, String timeISOStringFormat, List<WebNodeDbo> nodes, int upNodeCount) {
		
		checkSize(jsonRow);
		
		String tableName = jsonRow.get("_tableName");
		KeyToTableName info = keyValue.getValue();
		if(info == null) {
			if (log.isInfoEnabled())
				log.info("user="+user+" has no access to table="+tableName);
			throw new Unauthorized("user="+user+" has no access to table="+tableName);
		}

		boolean isUpdate = false;
		Object updateStr = jsonRow.get("_update");
		if("true".equals(updateStr) || Boolean.TRUE.equals(updateStr))
			isUpdate = true;

		DboTableMeta table = info.getTableMeta();
		Object pkValue = jsonRow.get(table.getIdColumnMeta().getColumnName());
		if(pkValue == null) {
			if (log.isWarnEnabled())
        		log.warn("The table you are inserting requires column='"+table.getIdColumnMeta().getColumnName()+"' to be set and is not found in json request="+jsonRow);
			throw new BadRequest("The table you are inserting requires column='"+table.getIdColumnMeta().getColumnName()+"' to be set and is not found in json request="+jsonRow);
		}
		
		//part of short term fix to put date formatting in and have it look like 'upload module'
		if (timeIsISOFormat && table.getIdColumnMeta().getColumnName().equals(timeISOFormatColumn))
			pkValue = getTimeAsMillisFromString((String)pkValue, timeISOStringFormat);

		TableKey theKey = new TableKey(tableName, pkValue);
		if(keyCheck.contains(theKey))
			throw new BadRequest("Your post contains two rows with the same key and table name.  key="+pkValue+" tablename="+tableName+".  This is not allowed");
		keyCheck.add(theKey);
		
		if(table.isTimeSeries())
			postTimeSeries(jsonRow, info, table, pkValue, timeIsISOFormat);
		else
			postNormalTable(jsonRow, solrDocs, info, isUpdate, table, pkValue, timeIsISOFormat, timeISOFormatColumn, timeISOStringFormat);

		PostTrigger trig = PostTrigger.transform(table);
		if(!StringUtils.isEmpty(trig.getScriptLanguage())) {
			TriggerRunnable trigger = new TriggerRunnable(trig, jsonRow, nodes, upNodeCount);
			ExecutorsSingleton.executor.execute(trigger);
		}
	}

	private static void postTimeSeries(Map<String, String> json,
			KeyToTableName info, DboTableMeta table, Object pkValue, boolean timeIsISOFormat) {
		Collection<DboColumnMeta> colsCol = table.getAllColumns();
		List<DboColumnMeta> cols = new ArrayList<DboColumnMeta>(colsCol);
		List<StorageTypeEnum> colTypes = new ArrayList<StorageTypeEnum>();


		List<Object> nodes = new ArrayList<Object>();
		for (DboColumnMeta col:cols) {
			Object node = json.get(col.getColumnName());
			if(node == null) {
				if (log.isWarnEnabled())
					log.warn("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in json request="+json);
				throw new BadRequest("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in json request="+json);
			}
			colTypes.add(col.getStorageType());
			nodes.add(node);
		}
		
		BigInteger timeStamp = new BigInteger(""+pkValue);
		long longTime = timeStamp.longValue();
		Long partitionSize = table.getTimeSeriesPartionSize();
		long partitionKey = calculatePartitionId(longTime, partitionSize);
		putPartition(NoSql.em(), table, partitionKey);
				
		if (cols.size() > 1) {
			LinkedHashMap<DboColumnMeta, Object> newValue = stringsToObjects(cols, colTypes, nodes);
			postRelationalTimeSeriesImplAssumingPartitionExists(NoSql.em(), table, timeStamp, newValue, colTypes, timeIsISOFormat, partitionKey);
		}
		else {
			Object newValue = stringToObject(cols.get(0).getStorageType(), nodes.get(0));
			postTimeSeriesImplAssumingPartitionExists(NoSql.em(), table, timeStamp, newValue, colTypes.get(0), timeIsISOFormat, partitionKey);
		}
	}
	
	
	public static void postTimeSeriesImplAssumingPartitionExists(NoSqlEntityManager mgr, DboTableMeta table, BigInteger pkValue, Object newValue, StorageTypeEnum type, boolean timeIsISOFormat, long partitionKey) {

		if (timeIsISOFormat)
			throw new BadRequest("Currently Iso Date Format is not supported with the TIME_SERIES table type");
		NoSqlTypedSession typedSession = mgr.getTypedSession();		
		String cf = table.getColumnFamily();

		DboColumnMeta idColumnMeta = table.getIdColumnMeta();

		//byte[] colKey = idColumnMeta.convertToStorage2(pkValue);
		byte[] colKey = toBytes(StorageTypeEnum.INTEGER, pkValue, idColumnMeta);
		TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
		BigInteger rowKey = new BigInteger(""+partitionKey);
		row.setRowKey(rowKey);
		
		Collection<DboColumnMeta> cols = table.getAllColumns();
		DboColumnMeta col = cols.iterator().next();
		//byte[] val = col.convertToStorage2(newValue);
		byte[] val = toBytes(type, newValue, col);
		row.addColumn(colKey, val, null);

		//This method also indexes according to the meta data as well
		typedSession.put(cf, row);

	}
	
	public static void postRelationalTimeSeriesImplAssumingPartitionExists(NoSqlEntityManager mgr, DboTableMeta table, BigInteger pkValue, LinkedHashMap<DboColumnMeta, Object> newValues, List<StorageTypeEnum> colTypes, boolean timeIsISOFormat, long partitionKey) {
		if (timeIsISOFormat)
			throw new BadRequest("Currently Iso Date Format is not supported with the RELATIONAL_TIME_SERIES table type");
		
		NoSqlTypedSession typedSession = mgr.getTypedSession();		
		String cf = table.getColumnFamily();

		DboColumnMeta idColumnMeta = table.getIdColumnMeta();
		
		//byte[] colKey = idColumnMeta.convertToStorage2(pkValue);
		byte[] colKey = toBytes(StorageTypeEnum.INTEGER, pkValue, idColumnMeta);
		TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
		BigInteger rowKey = new BigInteger(""+partitionKey);
		row.setRowKey(rowKey);

		ByteArrayOutputStream valStream = new ByteArrayOutputStream();

		try {
			int colinx = 0;
			for (Entry<DboColumnMeta, Object> entry:newValues.entrySet()) {
				DboColumnMeta col = entry.getKey();
				Object newValue = entry.getValue();
				//byte[] valarr = col.convertToStorage2(newValue);
				byte[] valarr = toBytes(colTypes.get(colinx), newValue, col);

				byte[] lenarr = ByteBuffer.allocate(4).putInt(valarr.length).array();
				//byte len = (byte)valarr.length;
				valStream.write(lenarr);
				valStream.write(valarr, 0, valarr.length);
				colinx++;
			}
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		row.addColumn(colKey, valStream.toByteArray(), null);
		typedSession.put(cf, row);
	}
	
	//this is a replacement for "convertToStorage2" which is quite slow.
	//it takes the cases we care about (apx 100% of our data), and does it the quick way
	private static byte[] toBytes(StorageTypeEnum type, Object source, DboColumnMeta fallbackmeta) {
		if(source == null)
			return null;
		
		switch (type) {
			case STRING: return ((String)source).getBytes(charset);
			case INTEGER: return ((BigInteger)source).toByteArray();
			case DECIMAL: 	BigDecimal value = (BigDecimal) source;
							BigInteger bi = value.unscaledValue();
							Integer scale = value.scale();
							byte[] bibytes = bi.toByteArray();
							byte[] sbytes = intToBytes(scale);
							byte[] bytes = new byte[bi.toByteArray().length+4];
							
							for (int i = 0 ; i < 4 ; i++) bytes[i] = sbytes[i];
							for (int i = 4 ; i < bibytes.length+4 ; i++) bytes[i] = bibytes[i-4];
				
							return bytes;
			default: return fallbackmeta.convertToStorage2(source);
		}
	}
	
	private static byte[] intToBytes(int val) {
		try {
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(outBytes);
			out.writeInt(val);
			byte[] outData = outBytes.toByteArray();
			return outData;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	//this is a replacement for "convertFromStorage2" which is quite slow.
	//it takes the cases we care about (apx 100% of our data), and does it the quick way
	private static Object fromBytes(StorageTypeEnum type, byte[] source, DboColumnMeta fallbackmeta) {
		if(source == null)
			return null;
		else if(source.length == 0)
			return null;
		switch (type) {
			case STRING: return new String(source, charset);
			case INTEGER: return new BigInteger(source);
			case DECIMAL: 	ByteBuffer buf = ByteBuffer.wrap(source);
							int scale = buf.getInt();
							byte[] bibytes = new byte[buf.remaining()];
							buf.get(bibytes);
							 
							BigInteger bi = new BigInteger(bibytes);
							return new BigDecimal(bi,scale);
			default: return fallbackmeta.convertFromStorage2(source);
		}
	}


	public static void putPartition(NoSqlEntityManager mgr, DboTableMeta table, long partitionKey) {
		NoSqlTypedSession typedSession = mgr.getTypedSession();		

		TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
		BigInteger rowKey = new BigInteger(""+partitionKey);
		row.setRowKey(rowKey);

		DboTableMeta meta = mgr.find(DboTableMeta.class, "partitions");
		byte[] partitionsRowKey = StandardConverters.convertToBytes(table.getColumnFamily());
		byte[] partitionBytes = StandardConverters.convertToBytes(rowKey);
		Column partitionIdCol = new Column(partitionBytes, null);
		NoSqlSession session = mgr.getSession();
		List<Column> columns = new ArrayList<Column>();
		columns.add(partitionIdCol);
		session.put(meta, partitionsRowKey, columns);
	}


	public static long calculatePartitionId(long longTime, Long partitionSize) {
		long partitionId = (longTime / partitionSize) * partitionSize;
		if(partitionId < 0) {
			//if partitionId is less than 0, it incorrectly ends up in the higher partition -20/50*50 = 0 and 20/50*50=0 when -20/50*50 needs to be -50 partitionId
			if(Long.MIN_VALUE+partitionSize >= partitionId)
				partitionId = Long.MIN_VALUE;
			else
				partitionId -= partitionSize; //subtract one partition size off of the id
		}

		return partitionId;
	}

	private static void postNormalTable(Map<String, String> json,
			Collection<SolrInputDocument> solrDocs, KeyToTableName info,
			boolean isUpdate, DboTableMeta table, Object pkValue,
			boolean timeIsISOFormat, String timeISOFormatColumn, String timeISOStringFormat) {
		if (log.isInfoEnabled())
			log.info("normal table name = '" + table.getColumnFamily() + "'");

		NoSqlTypedSession typedSession = NoSql.em().getTypedSession();
		
		DboColumnMeta idColumnMeta = table.getIdColumnMeta();
		Object rowKey = stringToObject(idColumnMeta.getStorageType(), pkValue);
		String cf = table.getColumnFamily();
//		TypedRow row = typedSession.find(cf, rowKey);
//		if(row == null) {
			//create new row
			TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
			row.setRowKey(rowKey);			
//		} else if(!isUpdate) {
//			if (log.isWarnEnabled())
//        		log.warn("pk already in use="+pkValue+" table="+tableName+" user needs to use _update=true in their json");
//			throw new BadRequest("This row for table="+tableName+" and primary key="+pkValue+" already exists.  Use _update=true in your json if you really want to modify this value");
//		}

		Collection<DboColumnMeta> cols = table.getAllColumns();
		
		long timestamp = System.currentTimeMillis();
		for(DboColumnMeta col : cols) {
			Object node = json.get(col.getColumnName());
			if(node == null) {
				if (log.isWarnEnabled())
	        		log.warn("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in json request="+json);
				throw new BadRequest("The table you are inserting requires column='"+col.getColumnName()+"' to be set and is not found in json request="+json);
			}

			if (timeIsISOFormat && StringUtils.equals(col.getColumnName(), timeISOFormatColumn))
				node = getTimeAsMillisFromString((String)node, timeISOStringFormat);

			addColumnData(isUpdate, row, col, node, timestamp);
		}

		addHistoryInfo(isUpdate, row, timestamp, json);
		
		//This method also indexes according to the meta data as well
		typedSession.put(cf, row);
		
		SecureTable sdiTable = info.getSdiTableMeta();
		SearchPosting.addSolrDataDoc(row, sdiTable, solrDocs);
	}

	private static String getTimeAsMillisFromString(String node, String format) {
		DateTimeFormatter parser = ISODateTimeFormat.basicDateTimeNoMillis();
		if (StringUtils.isNotBlank(format)) {
			try {
				parser = DateTimeFormat.forPattern(format);
			}
			catch (IllegalArgumentException iae) {
				throw new RuntimeException("The date time format you provided is not legal, see this page for valid date formatting http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html");
			}
		}
		Long UtcMillis = parser.parseDateTime(node).getMillis();
		return ""+UtcMillis;
		
	}


	private static void addColumnData(boolean isUpdate, TypedRow row, DboColumnMeta col, Object node, long time) {
		Object newValue = stringToObject(col.getStorageType(), node);
		if(isUpdate) {
			//okay, we are updating a row so we need to version this row then so we have the previous contents of the row as well
			transferColToHistory(row, col, newValue, time);
		}
		
		row.addColumn(col.getColumnName(), newValue);
	}

	private static void transferColToHistory(TypedRow row, DboColumnMeta col, Object newValue, long time) {
		String colName = col.getColumnName();
		TypedColumn column = row.getColumn(colName);
		if(column == null)
			return; //nothing to do since it doesn't exist

		Object oldValue = column.getValue();
		if(newValue.equals(oldValue))
			return; //nothing to do if values are the same
		
		//okay, the value was changed so we must save the oldValue in the history then
		//the name is _history.<timestamp>.<columnname> for each column that is changed
		byte[] namePrefix = StandardConverters.convertToBytes(HISTORY_PREFIX);
		byte[] nameAsBytes = col.getColumnNameAsBytes();

		byte[] name = DocumentUtil.combineName(namePrefix, time, nameAsBytes);
		byte[] valueRaw = column.getValueRaw();
		row.addColumn(name, valueRaw, null);
	}

	private static void addHistoryInfo(boolean isUpdate, TypedRow row, long time, Map<String, String> json) {
		String state = json.get(STATE_COL_NAME);
		String user = json.get("_user");
		String description = json.get("_description");
		byte[] namePrefix = StandardConverters.convertToBytes(HISTORY_PREFIX);
		byte[] stateColName = StandardConverters.convertToBytes(STATE_COL_NAME);
		byte[] name = DocumentUtil.combineName(namePrefix, time, stateColName);

		if(isUpdate) {
			transferToHistory(row, stateColName, name);
		}

		//If user supplies any values add the column
		if(state != null || user != null || description != null) {
			byte[] valueRaw = DocumentUtil.createDocument(state, user, description);
			row.addColumn(stateColName, valueRaw, null);
		} else {
			//null out the column in case it existed before...
			row.removeColumn(stateColName);
		}
	}

	private static void transferToHistory(TypedRow row, byte[] stateColName, byte[] historyColName) {
		TypedColumn column = row.getColumn(stateColName);
		if(column == null)
			return;

		byte[] valueRaw = column.getValueRaw();
		row.addColumn(historyColName, valueRaw, null);
	}
	
	public static LinkedHashMap<DboColumnMeta, Object> stringsToObjects(List<DboColumnMeta> cols, List<StorageTypeEnum> types, List<Object> someVals) {
		LinkedHashMap<DboColumnMeta, Object> result = new LinkedHashMap<DboColumnMeta, Object>();
		if(CollectionUtils.size(cols) ==0 || CollectionUtils.size(someVals)==0)
			return result;
		for (int i = 0; i<cols.size(); i++) {
			
			DboColumnMeta col = cols.get(i);
			Object someVal = someVals.get(i);
			result.put(col, stringToObject(types.get(i), someVal));		
		}
		return result;
	}

	public static Object stringToObject(StorageTypeEnum type, Object someVal) {
		
		if(someVal == null)
			return null;
		else if("null".equals(someVal))
			return null; //a fix for when they pass us "null" instead of null
		
		String val = ""+someVal;
		if(val.length() == 0)
			val = null;
		switch (type) {
			case DECIMAL: return new BigDecimal(val);
			case INTEGER: return new BigInteger(val);
			default: return val;
		}
		
	}
	
	private static void checkSize(Map<String, String> jsonMap) {
		//JsonNode jsonNode = mapper.valueToTree(jsonMap);
		StringWriter strWriter = new StringWriter();
		try {
			mapper.writeValue(strWriter, jsonMap);
			String json = strWriter.toString();
			if(json.length() > 5000) {
				if (log.isInfoEnabled())
					log.info("json data point larger than 5000");
				throw new BadRequest("You have a single data point in your batch that is larger than 5000 characters which is not allowed.  If you need this contact us to raise limit");
			}
		} catch (JsonGenerationException e) {
			if (log.isWarnEnabled())
        		log.warn("parse issue1", e);
			throw new Error("Bug marshalling out to json");
		} catch (JsonMappingException e) {
			if (log.isWarnEnabled())
        		log.warn("parse issue2", e);
			throw new Error("Bug marshalling out to json");
		} catch (IOException e) {
			if (log.isWarnEnabled())
        		log.warn("parse issue3", e);
			throw new Error("Bug marshalling out to json");
		}
	}
}
