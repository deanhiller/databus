package controllers.modules2;

import gov.nrel.util.Utility;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import models.EntityUser;
import models.SecureTable;

import org.apache.cassandra.db.SuperColumn;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.io.JsonStringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
import play.mvc.results.BadRequest;
import play.mvc.results.Unauthorized;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.conv.StorageTypeEnum;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;
import com.google.inject.Provider;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.RequestBuilder;

import controllers.modules2.framework.Config;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.AHttpChunkingListener;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.ProcessorSetupAbstract;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;
import controllers.modules2.framework.translate.TranslationFactory;

public class RawProcessor extends ProcessorSetupAbstract implements PullProcessor, EndOfChain {

	private static final Logger log = LoggerFactory.getLogger(RawProcessor.class);
	private Iterator<KeyValue<TypedRow>> cursor;
	private DboTableMeta meta;
	private BigDecimal maxInt = new BigDecimal(Integer.MAX_VALUE);
	
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
		String colFamily = parameters.get(0);
		Long start = params.getOriginalStart();
		Long end = params.getOriginalEnd();
		
		NoSqlEntityManager em = NoSql.em();

		SecureTable sdiTable = SecureTable.findByName(NoSql.em(), colFamily);
		if(sdiTable == null)
			throw new BadRequest("table="+colFamily+" does not exist");
		
		meta = sdiTable.getTableMeta();
		String cf = meta.getColumnFamily();
		String idCol = meta.getIdColumnMeta().getColumnName();
		String sql = "SELECT c FROM "+cf+" as c WHERE c."+idCol+" >= "+start+" and c."+idCol+" <= "+end;
		if (log.isInfoEnabled())
			log.info("Setting up for sql="+sql);
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
			return new ReadResult(getUrl(), ""+kv.getException().getMessage());
		}
		TypedRow row = kv.getValue();
		TSRelational tv = new TSRelational();
		DboColumnMeta idMeta = meta.getIdColumnMeta();
		if(row.getRowKey() == null)
			return new ReadResult(getUrl(), "rowkey="+kv.getKey()+" found in index, but row not found");

		Object key = row.getRowKey();
		String colName = idMeta.getColumnName();
		tv.put(colName, key);
		for(DboColumnMeta colMeta : meta.getAllColumns()) {
			createJson(row, colMeta, tv);
		}
		
		modifyForMaxIntBug(tv);
		
		return new ReadResult(getUrl(), tv);
	}

	private void modifyForMaxIntBug(TSRelational tv) {
		//because of a single client that puts in Integer.MAX_INT, we need to modify all Integer.MAX_INT to return null
		BigDecimal dec = getValueEvenIfNull(tv);
		if(maxInt.equals(dec)) {
			setValue(tv, null);
		}
	}

	private static void createJson(TypedRow row, DboColumnMeta colMeta, TSRelational tv) {
		String colName = colMeta.getColumnName();
		TypedColumn column = row.getColumn(colName);
		if(column != null) {
			Object data = column.getValue();
			tv.put(colName, data);
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
