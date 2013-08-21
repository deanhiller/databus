package controllers;

import java.util.Iterator;

import org.codehaus.jackson.io.JsonStringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F.Promise;

import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.conv.StorageTypeEnum;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;

class ApiOurJsonStreamer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ApiOurJsonStreamer.class);
	private Promise<String> out;
	private int count;
	private Iterator<KeyValue<TypedRow>> rows;
	private DboTableMeta tableMeta;
	private int batchSize;
	private String sql;
	private boolean hasError;
	
	public ApiOurJsonStreamer(String sql, DboTableMeta tableMeta, Iterator<KeyValue<TypedRow>> rows2,
			Promise<String> out, int count2, int batchSize) {
		this.sql = sql;
		this.batchSize = batchSize;
		this.tableMeta = tableMeta;
		this.rows = rows2;
		this.out = out;
		this.count = count2;
	}

	public int getCount() {
		return this.count;
	}

	public void run() {
		try {
			runImpl();
		} catch(Exception e) {
			if (log.isWarnEnabled())
        		log.warn("Exception streaming back file", e);
			hasError = true;
			out.invoke("BUG, ERROR processing request. exc type="+e.getClass()+" msg="+e.getMessage());
		}
	}
	
	public void runImpl() {
		if (ApiGetData.log.isInfoEnabled())
			ApiGetData.log.info("starting to run sql="+sql+" count="+count);
		
		//NoSqlEntityManager mgr = NoSql.em();
		//NoSqlTypedSession typedSession = mgr.getTypedSession();
//			{
//				"employees": [
//				{ "firstName":"John" , "lastName":"Doe" }, 
//				{ "firstName":"Anna" , "lastName":"Smith" }, 
//				{ "firstName":"Peter" , "lastName":"Jones" }
//				]
//			}

		String chunkOfData = "";
		int chunkCounter = 0;
		while(rows.hasNext() && chunkCounter < batchSize) {
			KeyValue<TypedRow> keyVal = rows.next();
			if(keyVal.getException() != null)
				continue;
			
			TypedRow row = keyVal.getValue();
			String json = translate(tableMeta, row);
			if(rows.hasNext()) {
				//for the very last row of data(last row in last batch), 
				//we must write it out with no comma at the end
				chunkOfData += json+",";
			} else
				chunkOfData += json;
			chunkCounter++;
		}
		
		count += chunkCounter;
		out.invoke(chunkOfData);
		if (ApiGetData.log.isInfoEnabled())
			ApiGetData.log.info("Total processed so far="+count+" num processed this chunk="+chunkCounter);
	}

	public static String translate(DboTableMeta meta, TypedRow row) {
		DboColumnMeta idMeta = meta.getIdColumnMeta();
		if(row.getRowKey() == null)
			return "{ }";
		String strVal = idMeta.convertTypeToString(row.getRowKey());
		
		boolean excludeQuotes = calculateExclusion(idMeta);
		String val = convert(strVal, excludeQuotes);
		String colNameVal = convert(idMeta.getColumnName(), false);
		String jsonStr = colNameVal+":"+val+",";
		
		int i = 0;
		for(DboColumnMeta colMeta : meta.getAllColumns()) {
			i++;
			jsonStr += createJson(row, colMeta);
			if(i < meta.getAllColumns().size())
				jsonStr += ",";
		}
		
		return "{"+jsonStr+"}";
	}

	private static String createJson(TypedRow row, DboColumnMeta colMeta) {
		String colName = colMeta.getColumnName();
		TypedColumn column = row.getColumn(colName);
		String strVal = null;
		boolean excludeQuotes = false;
		if(column != null) {
			Object data = column.getValue();
			strVal = colMeta.convertTypeToString(data);
			
			excludeQuotes = calculateExclusion(colMeta);
		}
		
		String val = convert(strVal, excludeQuotes);
		String colNameVal = convert(colName, false);
		String jsonSnippet = colNameVal+":"+val;
		return jsonSnippet;
	}

	public static boolean calculateExclusion(DboColumnMeta colMeta) {
		if(colMeta.getStorageType() == StorageTypeEnum.DECIMAL
				|| colMeta.getStorageType() == StorageTypeEnum.INTEGER
				|| colMeta.getStorageType() == StorageTypeEnum.BOOLEAN)
			return true;
		return false;
	}

	public static String convert(String originalVal, boolean excludeQuotes) {
		if(originalVal == null)
			return "null";
		else if(excludeQuotes) //we don't quote numbers nor booleans and no need to escape them.
			return originalVal;
		JsonStringEncoder inst = JsonStringEncoder.getInstance();
		char[] withEscapes = inst.quoteAsString(originalVal);
		String val = new String(withEscapes);
		return "\""+val+"\"";
	}

	public boolean hasError() {
		return hasError;
	}
}