package controllers.modules2;

import java.math.BigDecimal;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nrel.util.Utility;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;

public class RawStreamProcessor implements RawSubProcessor {

	private static final Logger log = LoggerFactory.getLogger(RawStreamProcessor.class);
	private Iterator<KeyValue<TypedRow>> cursor;
	private DboTableMeta meta;
	private String url;
	
	@Override
	public void init(DboTableMeta meta, Long start, Long end, String url, VisitorInfo visitor) {
		this.url = url;
		this.meta = meta;
		String cf = meta.getColumnFamily();
		String idCol = meta.getIdColumnMeta().getColumnName();
		String sql = "SELECT c FROM "+cf+" as c WHERE c."+idCol+" >= "+start+" and c."+idCol+" <= "+end;
		if (log.isInfoEnabled())
			log.info("Setting up for sql="+sql);
		
		NoSqlEntityManager em = NoSql.em();
		NoSqlTypedSession typedSession = em.getTypedSession();
		QueryResult result = typedSession.createQueryCursor(sql, 500);
		Cursor<KeyValue<TypedRow>> cursor = result.getPrimaryViewCursor();
		Iterable<KeyValue<TypedRow>> proxy = Utility.fetchProxy(cursor);

		this.cursor = proxy.iterator();		
	}

	@Override
	public ReadResult read() {
		if(!cursor.hasNext())
			return new ReadResult();

		KeyValue<TypedRow> next = cursor.next();
		return translate(meta, next);
	}

	public ReadResult translate(DboTableMeta meta, KeyValue<TypedRow> kv) {
		if(kv.getException() != null) {
			return new ReadResult(url, ""+kv.getException().getMessage());
		}
		TypedRow row = kv.getValue();
		//TODO:  parameterize timeColumn and valueColumn from options
		TSRelational tv = new TSRelational("time", "value");
		DboColumnMeta idMeta = meta.getIdColumnMeta();
		if(row.getRowKey() == null)
			return new ReadResult(url, "rowkey="+kv.getKey()+" found in index, but row not found");

		Object key = row.getRowKey();
		String colName = idMeta.getColumnName();
		tv.put(colName, key);
		for(DboColumnMeta colMeta : meta.getAllColumns()) {
			createJson(row, colMeta, tv);
		}
		
		return new ReadResult(url, tv);
	}

	private static void createJson(TypedRow row, DboColumnMeta colMeta, TSRelational tv) {
		String colName = colMeta.getColumnName();
		TypedColumn column = row.getColumn(colName);
		if(column != null) {
			Object data = column.getValue();
			tv.put(colName, data);
		}
	}
}
