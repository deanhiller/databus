package controllers.api;

import gov.nrel.util.SearchUtils;
import gov.nrel.util.Utility;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import models.DataTypeEnum;
import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.KeyToTableName;
import models.MetaDataTag;
import models.PermissionType;
import models.RoleMapping;
import models.SdiColumn;
import models.SdiColumnProperty;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;
import models.message.DatasetColumnModel;
import models.message.DatasetColumnSemanticModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;
import models.message.SearchableParent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import play.Play;
import play.mvc.Util;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z8spi.meta.DboColumnCommonMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnToOneMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.play.NoSql;

import controllers.SearchPosting;
import controllers.SecurityUtil;

public class ApiRegistrationImpl {

	private static final Logger log = LoggerFactory.getLogger(ApiRegistrationImpl.class);
	public static int divisor = Integer.MAX_VALUE / 10;

	public static RegisterResponseMessage registerImpl(RegisterMessage msg, String username, String apiKey) {
		
		if (log.isInfoEnabled())
			log.info("Registering table="+msg.getModelName());
		if(msg.getDatasetType() != DatasetType.STREAM && msg.getDatasetType() != DatasetType.RELATIONAL_TABLE 
				&& msg.getDatasetType() != DatasetType.TIME_SERIES && msg.getDatasetType() != DatasetType.RELATIONAL_TIME_SERIES) {
			if (log.isInfoEnabled())
				log.info("only supporting type of STREAM or RELATIONAL_TABLE or TIME_SERIES right now");
			throw new BadRequest("only supporting type of STREAM or RELATIONAL_TABLE or TIME_SERIES right now");
		} else if(msg.getModelName() == null) {
			if (log.isInfoEnabled())
				log.info("model name was null and is required");
			throw new BadRequest("model name was null and is required");
		} else if(!DboTableMeta.isValidTableName(msg.getModelName())) {
			if (log.isInfoEnabled())
				log.info("modelName is invalid, it much match regular expression=[a-zA-Z_][a-zA-Z_0-9]*");
			throw new BadRequest("modelName is invalid, it much match regular expression=[a-zA-Z_][a-zA-Z_0-9]*");
		}

		String schemaName = null;
		if((msg.getGroups()!=null && msg.getGroups().size()!=0) && msg.getSchema() != null) {
			if (log.isInfoEnabled())
				log.info("use supplied groups and schema attribute...this is not allowed");
			throw new BadRequest("Cannot supply schema AND list of groups.  groups is deprecated, use schema only");
		} else if(msg.getGroups() != null && msg.getGroups().size() > 0) {
			if(msg.getGroups().size() > 1) {
				if (log.isInfoEnabled())
					log.info("more than one group is not allowed");
				throw new BadRequest("Cannot have more than one group anymore.  That is deprecated");
			}

			schemaName = msg.getGroups().get(0);
		} else if(msg.getSchema() != null) {
			schemaName = msg.getSchema();
		} else {
			if (log.isInfoEnabled())
				log.info("missing schema attribute");
			throw new BadRequest("Missing the schema attribute which is required");
		}

		//STREAM no longer exists
		if(msg.getDatasetType() == DatasetType.STREAM)
			msg.setDatasetType(DatasetType.TIME_SERIES);

		DatasetType type = msg.getDatasetType();
		validateColumnsForDatasetType(msg);
		ensureTableDoesNotAlreadyExist(msg);
		EntityUser user = SecurityUtil.fetchUser(username, apiKey);

		SecureSchema schema = SecureSchema.findByName(NoSql.em(), schemaName);
		if(schema == null)
			throw new BadRequest("you must specify an existing schema or group");
		
		// Setup the table meta
		DboTableMeta tm = new DboTableMeta();
		if(msg.getDatasetType() == DatasetType.TIME_SERIES) {
			List<DatasetColumnModel> columns = msg.getColumns();
			if(columns.size() != 2)
				throw new BadRequest("TIME_SERIES table can only have two columns, the primary key of long(time since epoch) and value of any type");

			String modelName = msg.getModelName();
			String realCf = Utility.createCfName(modelName);
			long partitionSize = TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
			tm.setTimeSeries(true);
			tm.setTimeSeriesPartionSize(partitionSize);
			tm.setup(msg.getModelName(), realCf, false, null);
			tm.setColNameType(long.class);

		} else if  (msg.getDatasetType() == DatasetType.RELATIONAL_TIME_SERIES) {
			String modelName = msg.getModelName();
			String realCf = Utility.createCfName(modelName);
			long partitionSize = TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
			tm.setTimeSeries(true);
			tm.setTimeSeriesPartionSize(partitionSize);
			tm.setup(msg.getModelName(), realCf, false, null);
			tm.setColNameType(long.class);
		} else {
			String dataCf = "relational";
			tm.setup(msg.getModelName(), dataCf, false, null);
		}

		// create new Table here and add to security group as well
		SecureTable t = new SecureTable();
		if(msg.getDatasetType() == DatasetType.RELATIONAL_TABLE) {
			t.setTypeOfData(DataTypeEnum.RELATIONAL);
		} else if(msg.getDatasetType() == DatasetType.TIME_SERIES) {
			t.setTypeOfData(DataTypeEnum.TIME_SERIES);
		}
		else if(msg.getDatasetType() == DatasetType.RELATIONAL_TIME_SERIES) {
			t.setTypeOfData(DataTypeEnum.RELATIONAL_TIME_SERIES);
		}

		
		
		
		NoSql.em().fillInWithKey(t);
		
		List<MetaDataTag>newtags = new ArrayList<MetaDataTag>();
		if (msg.getTags() != null && msg.getTags().size() > 0) {
			for (String s: msg.getTags()) {
				newtags.add(MetaDataTag.getMetaDataTag(s, 1, NoSql.em()));
			}
		}
			

		t.setTableName(msg.getModelName());
		t.setDateCreated(new LocalDateTime());
		t.setTableMeta(tm);
		t.setCreator(user);
		t.setIsForLogging(msg.getIsForLogging());
		t.setSearchable(msg.getIsSearchable());
		
		long before = System.currentTimeMillis();
		//------ real code
		t.setTags(newtags, NoSql.em());
//		for (MetaDataTag tag:t.getTags()){
//			NoSql.em().put(tag);
//		}
		//----- end real code
		long after = System.currentTimeMillis();
		log.warn("^^^^^ time for setting tags: "+(after-before));
		
		
		schema.addTable(t);

		tm.setForeignKeyToExtensions(t.getId());

		setupEachColumn(msg, t, type);
		setupParentTables(msg, t, type);

		NoSqlEntityManager mgr = NoSql.em();

		if (log.isInfoEnabled())
			log.info("table id = '" + tm.getColumnFamily() + "'");
		
		setApiKeysForEntities(schema.getEntitiesWithAccess(), t, tm);
		mgr.put(schema);
	
		mgr.put(tm);
		mgr.put(t);

		if(t.isSearchable()) {
			try {
				ArrayList<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
				SearchUtils.indexTable(t, tm, solrDocs);
				SearchPosting.saveSolr("table id = '" + tm.getColumnFamily() + "'", solrDocs, "databusmeta");
				solrDocs = new ArrayList<SolrInputDocument>();
			}
			catch(Exception e) {
				if (log.isInfoEnabled())
					log.info("Got an exception preparing solr for a new searchable table - "+e.getMessage());
				throw new RuntimeException("Got an exception preparing solr for a new searchable table, cancelling creation of table.  Try to reregister at a later time when solr is back online or make your table isSearchable=false..", e);
			}
		}
	
		mgr.flush();
		

		if (log.isInfoEnabled())
			log.info("Registered table="+msg.getModelName());
		// Construct the response message
		RegisterResponseMessage rmsg = new RegisterResponseMessage();
		rmsg.setModelName(msg.getModelName());

		return rmsg;
	}

	private static void setupParentTables(RegisterMessage msg, SecureTable t,
			DatasetType type) {
		List<SearchableParent> parents = msg.getSearchableParent();
		//we're only allowing one for now:
		if (parents!=null && parents.size()!=0) {
			SearchableParent sp = parents.get(0);
			//it's ok if they are all blank:
			if (!allOrNoneBlank(sp.getParentFKField(), sp.getParentTable(), sp.getChildFKField()))
				throw new BadRequest("If searchableParent is specified then all fields must be specified:  parentTable, childFKField, parentFKField");
			
			t.setSearchableParentTable(sp.getParentTable());
			t.setSearchableParentParentCol(sp.getParentFKField());
			t.setSearchableParentChildCol(sp.getChildFKField());
		}
		
	}
	
	private static boolean allOrNoneBlank(String... strings) {
		boolean firstTime = true;
		boolean checkingNulls = false;
		for (String s:strings) {
			if (firstTime) {
				if (StringUtils.isBlank(s))
					checkingNulls=true;
				firstTime=false;
			}
			else if (checkingNulls && !StringUtils.isBlank(s)) {
				return false;
			}
			else if (!checkingNulls && StringUtils.isBlank(s)) {
				return false;
			}
		}
		return true;
	}
	
	private static void validateColumnsForDatasetType(RegisterMessage msg) {
		//a STREAM dataset type must only contain columns 'time' of type bigInteger and 'value' of type bigDecimal:
		if(msg.getDatasetType() == DatasetType.STREAM || msg.getDatasetType() == DatasetType.TIME_SERIES) {
			if (msg.getColumns().size() == 2) {
				DatasetColumnModel c1=msg.getColumns().get(0);
				DatasetColumnModel c2=msg.getColumns().get(1);
				if("time".equalsIgnoreCase(c1.getName()) && "BigInteger".equalsIgnoreCase(c1.getDataType()) &&
						"value".equalsIgnoreCase(c2.getName()) && "BigDecimal".equalsIgnoreCase(c2.getDataType()))
					return;
				if("time".equalsIgnoreCase(c2.getName()) && "BigInteger".equalsIgnoreCase(c2.getDataType()) &&
						"value".equalsIgnoreCase(c1.getName()) && "BigDecimal".equalsIgnoreCase(c1.getDataType()))
					return;
			}
			throw new BadRequest("When registering a STREAM the only columns allowed are 'time' of type 'BigInteger' and 'value' of type 'BigDecimal'");
		}
				

		
	}

	private static void setApiKeysForEntities(List<SecureResourceGroupXref> entitiesWithAccess, SecureTable table, DboTableMeta tm) {
		Set<String> usedIds = new HashSet<String>();
		for(SecureResourceGroupXref ref : entitiesWithAccess) {
			Entity userOrGroup = ref.getUserOrGroup();
			setNonMatchingApiKeys(userOrGroup, table, tm, usedIds);
		}
	}

	private static void setNonMatchingApiKeys(Entity e, SecureTable table, DboTableMeta tm,
			Set<String> usedIds) {
		//The following code is ONLY if groups have a circular reference which should never
		//happen but it protects on circular reference if group A belongs to group B belongs to 
		//groupC belongs to groupA...
		String id = e.getId();
			if(usedIds.contains(id))
				return; //skip already processed entities
			
		usedIds.add(id);
		
		if (e.getClassType().equals("userImpl")) {
			EntityUser u = (EntityUser)e;

			String rowKey2 = KeyToTableName.formKey(table.getTableName(), u.getUsername(), u.getApiKey());
			KeyToTableName k2 = createKeyToTable(rowKey2, tm, table);
			NoSql.em().put(k2);
		} else if(e.getClassType().equals("group")) {
			EntityGroup group = (EntityGroup) e;
			List<EntityGroupXref> children = group.getChildren();
			for(EntityGroupXref xref : children) {
				Entity entity = xref.getEntity();
				setNonMatchingApiKeys(entity, table, tm, usedIds);
			}
		}
	}

	private static KeyToTableName createKeyToTable(String rowKey,
			DboTableMeta tm, SecureTable t) {
		KeyToTableName k2t = new KeyToTableName();
		k2t.setTableMeta(tm);
		k2t.setSdiTableMeta(t);
		k2t.setKey(rowKey);
		return k2t;
	}

	private static void ensureTableDoesNotAlreadyExist(RegisterMessage msg) {
		SecureResource t2 = SecureTable.findByName(NoSql.em(), msg.getModelName());
		DboTableMeta table = NoSql.em().find(DboTableMeta.class, msg.getModelName());
		if(t2 != null) {
			if (log.isInfoEnabled())
				log.info("Sorry, table with tablename='"+msg.getModelName()+"' is already in use");
			throw new BadRequest("Sorry, table with tablename='"+msg.getModelName()+"' is already in use");
		} else if (table != null) {
			if (log.isInfoEnabled())
				log.info("Table '" + msg.getModelName() + "' already exists (is there a bug here?).");
			throw new BadRequest("Table '" + msg.getModelName() + "' already exists (is there a bug here?).");
		} // if
	}

	//remove after 12/12/12
	private static Map<String, SecurityGroup> formAdminGroups(
			List<RoleMapping> roles) {
		Map<String, SecurityGroup> nameToGroup = new HashMap<String, SecurityGroup>();
		for(RoleMapping r : roles) {
			if(r.getPermission() == PermissionType.ADMIN)
				nameToGroup.put(r.getGroup().getName(), r.getGroup());
		}
		return nameToGroup;
	}

	private static void setupEachColumn(RegisterMessage msg, SecureTable t, DatasetType type) {
		boolean hasPrimaryKey = false;
		NoSqlEntityManager mgr = NoSql.em();
		DboTableMeta tm = t.getTableMeta();
		boolean tableIsIndexed = false;
		// Setup the column meta
		for (DatasetColumnModel c : msg.getColumns()) {
			if(c.isPrimaryKey) {
				if(hasPrimaryKey) {
					if (log.isInfoEnabled())
						log.info("You cannot have two columns set to primary key at this time");
					throw new BadRequest("You cannot have two columns set to primary key at this time");
				}
				hasPrimaryKey = true;
			}

			if(c.isIndex)
				tableIsIndexed = true;
			
			if(!DboColumnMeta.isValidColumnName(c.getName())) {
				if (log.isInfoEnabled())
					log.info("col name="+c.getName()+" is not valid.  must match regex=[a-zA-Z_][a-zA-Z_0-9]*");
				throw new BadRequest("col name="+c.getName()+" is not valid.  must match regex=[a-zA-Z_][a-zA-Z_0-9]*");
			}
			
			// Set up the semantic meta for this particular column
			String semanticType = c.getSemanticType();
			SdiColumn col = new SdiColumn();
			col.setColumnName(c.getName());
			if (c.isFacet)
				t.setFacetFields(t.getFacetFields()+","+c.getName());
			col.setSemanticType(semanticType);

			setupSemanticData(mgr, c, col);

			Class dataType = null;

			if (c.getDataType().equalsIgnoreCase("BigDecimal")
					||c.getDataType().equalsIgnoreCase("Decimal")) {
				dataType = BigDecimal.class;
			} else if (c.getDataType().equalsIgnoreCase("BigInteger")
					||c.getDataType().equalsIgnoreCase("Integer")) {
				dataType = BigInteger.class;
			} else if (c.getDataType().equalsIgnoreCase("String")) {
				dataType = String.class;
			} else if (c.getDataType().equalsIgnoreCase("Boolean")) {
				dataType = Boolean.class;
			} else if (c.getDataType().equalsIgnoreCase("bytes")) {
				dataType = byte[].class;
			} else {
				if (log.isInfoEnabled())
					log.info("user trying to register stream with type="+c.getDataType()+" which is not allowed");
				throw new BadRequest("Datatype="+c.getDataType()+" is not allowed.  Allowed values are bigdecimal/decimal, biginteger/integer, string, boolean, bytes(in hex)");
			}

			DboColumnMeta colMeta;
			if(c.getIsPrimaryKey()) {
				DboColumnIdMeta cm = formPrimaryKeyMeta(tm, c, dataType, type);
				t.setPrimaryKey(col);
				colMeta = cm;
				if (log.isInfoEnabled())
					log.info("col '" + cm.getColumnName() + "' is a pk " + cm.getId()+" isIndexed="+c.isIndex);
			} else {
				DboColumnMeta cm = formCommonColMeta(tm, c, dataType);
				t.getNameToField().put(c.getName(), col);
				colMeta = cm;
				if (log.isInfoEnabled())
					log.info("col '" + cm.getColumnName() + "' is NOT a pk " + cm.getId()+" isIndexed="+c.isIndex);
			}

			mgr.put(col);
			mgr.put(colMeta);
		} // for
		
		if(!tableIsIndexed) {
			if (log.isInfoEnabled())
				log.info("In your json request, NONE of the columns had isIndexed set to true and we require ONE index(and it can be primary key but doesn't have to be");
			throw new BadRequest("In your json request, NONE of the columns had isIndexed set to true and we require ONE index(and it can be primary key but doesn't have to be");
		} else if(!hasPrimaryKey) {
			if (log.isInfoEnabled())
				log.info("In your json request, NONE of the columns had primaryKey set to true and we require ONE primary key");
			throw new BadRequest("In your json request, NONE of the columns had primaryKey set to true and we require ONE primary key");
		}
	}

	private static DboColumnIdMeta formPrimaryKeyMeta(DboTableMeta tm, DatasetColumnModel c, Class dataType, DatasetType type ) {
		if(dataType != BigInteger.class && type == DatasetType.STREAM) {
			if (log.isInfoEnabled())
				log.info("primary key column was not of type integer yet this is time series data");
			throw new BadRequest("primary key column was not of type integer yet this is time series data");
		}

		boolean indexed = c.isIndex;
		if(type == DatasetType.TIME_SERIES || type == DatasetType.RELATIONAL_TIME_SERIES)
			indexed = false;
		DboColumnIdMeta idMeta = new DboColumnIdMeta();
		idMeta.setup(tm, c.getName(), dataType, indexed);
		return idMeta;
	}

	private static DboColumnMeta formCommonColMeta(DboTableMeta tm, DatasetColumnModel c, Class dataType) {
		DboColumnMeta result;
		String table = c.getForeignKeyTablename();
		if(table != null) {
			DboTableMeta fkTable = lookupFkTable(tm, table);
			DboColumnToOneMeta cm = new DboColumnToOneMeta();
			cm.setup(tm, c.getName(), fkTable, c.isIndex, false);
			result = cm;
		} else {
			DboColumnCommonMeta cm = new DboColumnCommonMeta();
			cm.setup(tm, c.getName(), dataType, c.isIndex, false);
			result = cm;
		}
		return result;
	}

	private static DboTableMeta lookupFkTable(DboTableMeta tm, String table) {
		DboTableMeta fkTable;
		if(table.equals(tm.getColumnFamily())) {
			fkTable = tm;
		} else {
			fkTable = NoSql.em().find(DboTableMeta.class, table);
			if(fkTable == null) {
				if (log.isInfoEnabled())
					log.info("They tried to create a table="+tm.getColumnFamily()+" that has fk to table="+table+" but that fk table does not exist and is not this table");
				throw new BadRequest("table="+table+" does not exist AND it's not this table so you can't create a foreign key to non-existent table");
			}
		}
		return fkTable;
	}

	private static void setupSemanticData(NoSqlEntityManager mgr,
			models.message.DatasetColumnModel c, SdiColumn col) {
		// Set up the additional semantic meta for this column, if any
		for (DatasetColumnSemanticModel s : c.getSemantics()) {

			final String property = s.getProperty();
			final String value = s.getValue();

			SdiColumnProperty cp = new SdiColumnProperty() {
				{
					setName(property);
					setValue(value);
				}
			};

			mgr.put(cp);
			col.getProperties().put(property, cp);

		} // for
	}
}
