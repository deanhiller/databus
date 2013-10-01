package controllers.modules2;

import gov.nrel.util.Utility;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import models.EntityUser;
import models.SecureTable;
import models.message.ChartVarMeta;

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

import controllers.SecurityUtil;
import controllers.modules2.framework.Config;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.EndOfChain;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.chain.AHttpChunkingListener;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.ProcessorSetupAbstract;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;
import controllers.modules2.framework.translate.TranslationFactory;

public class RawProcessor extends ProcessorSetupAbstract implements PullProcessor, EndOfChain {

	private static final Logger log = LoggerFactory.getLogger(RawProcessor.class);

	private BigDecimal maxInt = new BigDecimal(Integer.MAX_VALUE);
	private RawSubProcessor subprocessor;
	private boolean skipSecurity;

	private static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();
	private static MetaInformation metaInfo = new MetaInformation(parameterMeta, true);

	static {
		ChartVarMeta meta = new ChartVarMeta();
		meta.setLabel("Table");
		meta.setNameInJavascript("table");
		meta.setRequired(true);
		meta.setHelp("The table that we read data from");
		parameterMeta.put(meta.getNameInJavascript(), meta);
	}

	public RawProcessor() {
	}
	public RawProcessor(boolean skipSecurity) {
		this.skipSecurity = skipSecurity;
	}

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup child, boolean alreadyAdded) {
		return null;
	}
	
	@Override
	public MetaInformation getGuiMeta() {
		return metaInfo;
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, HashMap<String, String> options) {
		String res = super.init(path, nextInChain, visitor, options);
		List<String> parameters = params.getParams();
		if(parameters.size() == 0)
			throw new BadRequest("rawdata module requires a column family name");
		String colFamily = parameters.get(0);
		Long start = params.getOriginalStart();
		Long end = params.getOriginalEnd();

		SecureTable sdiTable = null;
		if(skipSecurity) {
			sdiTable = SecureTable.findByName(visitor.getMgr(), colFamily);
		} else
			sdiTable = SecurityUtil.checkSingleTable(colFamily);
		
		if(sdiTable == null)
			throw new BadRequest("table="+colFamily+" does not exist");

		DboTableMeta meta = sdiTable.getTableMeta();
		
		if(meta.isTimeSeries()) {
			if(visitor.isReversed())
				subprocessor = new RawTimeSeriesReversedProcessor();
			else
				subprocessor = new RawTimeSeriesProcessor();
		} else
			subprocessor = new RawStreamProcessor();
		
		subprocessor.init(meta, start, end, params.getPreviousPath(), visitor);
		return res;
	}

	@Override
	public void start(VisitorInfo visitor) {
		//nothing to do, engine is the one who starts reading from us
	}

	@Override
	public ReadResult read() {
		ReadResult r = subprocessor.read();
		TSRelational tv = r.getRow();
		if(tv != null)
			modifyForMaxIntBug(tv);
		return r;
	}

	private void modifyForMaxIntBug(TSRelational tv) {
		//because of a single client that puts in Integer.MAX_INT, we need to modify all Integer.MAX_INT to return null
		BigDecimal dec = getValueEvenIfNull(tv);
		if(maxInt.equals(dec)) {
			setValue(tv, null);
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
