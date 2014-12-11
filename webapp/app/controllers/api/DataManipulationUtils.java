package controllers.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;

import play.PlayPlugin;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.action.Column;
import com.alvazan.orm.api.z8spi.conv.StandardConverters;
import com.alvazan.orm.api.z8spi.conv.StorageTypeEnum;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;

public class DataManipulationUtils extends PlayPlugin {
	
	private final static Charset charset = StandardCharsets.UTF_8;

	
	public final static Object stringToObject(StorageTypeEnum type, Object someVal) {
		
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
	
	public final static LinkedHashMap<DboColumnMeta, Object> stringsToObjects(List<DboColumnMeta> cols, List<StorageTypeEnum> types, List<Object> someVals) {
		LinkedHashMap<DboColumnMeta, Object> result = new LinkedHashMap<DboColumnMeta, Object>();
		if(CollectionUtils.size(cols) ==0 || CollectionUtils.size(someVals)==0)
			return result;
		for (int i = 0; i<cols.size(); i++) {
			
			DboColumnMeta col = cols.get(i);
			Object someVal = someVals.get(i);
			result.put(col, DataManipulationUtils.stringToObject(types.get(i), someVal));		
		}
		return result;
	}
	
	
	public final static void postTimeSeriesImplAssumingPartitionExists(NoSqlEntityManager mgr, DboTableMeta table, BigInteger pkValue, Object newValue, StorageTypeEnum type, boolean timeIsISOFormat, BigInteger rowKey) {

		if (timeIsISOFormat)
			throw new BadRequest("Currently Iso Date Format is not supported with the TIME_SERIES table type");
		NoSqlTypedSession typedSession = mgr.getTypedSession();		
		String cf = table.getColumnFamily();

		DboColumnMeta idColumnMeta = table.getIdColumnMeta();

		//byte[] colKey = idColumnMeta.convertToStorage2(pkValue);
		byte[] colKey = toBytes(StorageTypeEnum.INTEGER, pkValue, idColumnMeta);
		TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
		row.setRowKey(rowKey);
		
		Collection<DboColumnMeta> cols = table.getAllColumns();
		DboColumnMeta col = cols.iterator().next();
		//byte[] val = col.convertToStorage2(newValue);
		byte[] val = toBytes(type, newValue, col);
		row.addColumn(colKey, val, null);

		
		//This method also indexes according to the meta data as well
		typedSession.put(cf, row);
	}
	
	public final static void postRelationalTimeSeriesImplAssumingPartitionExists(NoSqlEntityManager mgr, DboTableMeta table, BigInteger pkValue, LinkedHashMap<DboColumnMeta, Object> newValues, List<StorageTypeEnum> colTypes, boolean timeIsISOFormat, BigInteger rowKey) {
		if (timeIsISOFormat)
			throw new BadRequest("Currently Iso Date Format is not supported with the RELATIONAL_TIME_SERIES table type");
		
		NoSqlTypedSession typedSession = mgr.getTypedSession();		
		String cf = table.getColumnFamily();

		DboColumnMeta idColumnMeta = table.getIdColumnMeta();
		
		//byte[] colKey = idColumnMeta.convertToStorage2(pkValue);
		byte[] colKey = toBytes(StorageTypeEnum.INTEGER, pkValue, idColumnMeta);
		TypedRow row = typedSession.createTypedRow(table.getColumnFamily());
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
	public final static byte[] toBytes(StorageTypeEnum type, Object source, DboColumnMeta fallbackmeta) {
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
	
	public final static byte[] intToBytes(int val) {
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
	public final static Object fromBytes(StorageTypeEnum type, byte[] source, DboColumnMeta fallbackmeta) {
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


	public static void putPartition(NoSqlEntityManager mgr, String columnFamily, BigInteger rowKey) {
		NoSqlTypedSession typedSession = mgr.getTypedSession();		

		TypedRow row = typedSession.createTypedRow(columnFamily);
		row.setRowKey(rowKey);

		DboTableMeta meta = mgr.find(DboTableMeta.class, "partitions");
		byte[] partitionsRowKey = StandardConverters.convertToBytes(columnFamily);
		byte[] partitionBytes = StandardConverters.convertToBytes(rowKey);
		Column partitionIdCol = new Column(partitionBytes, null);
		NoSqlSession session = mgr.getSession();
		List<Column> columns = new ArrayList<Column>();
		columns.add(partitionIdCol);
		session.put(meta, partitionsRowKey, columns);
	}


	public static BigInteger calculatePartitionId(long longTime, long partitionSize) {
		long partitionId = (longTime / partitionSize) * partitionSize;
		if(partitionId < 0) {
			//if partitionId is less than 0, it incorrectly ends up in the higher partition -20/50*50 = 0 and 20/50*50=0 when -20/50*50 needs to be -50 partitionId
			if(Long.MIN_VALUE+partitionSize >= partitionId)
				partitionId = Long.MIN_VALUE;
			else
				partitionId -= partitionSize; //subtract one partition size off of the id
		}

		return new BigInteger(""+partitionId);
	}

	

}
