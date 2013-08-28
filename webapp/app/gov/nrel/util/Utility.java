package gov.nrel.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.KeyToTableName;
import models.PermissionType;
import models.RoleMapping;
import models.SdiColumn;
import models.SdiColumnProperty;
import models.SecureResource;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;
import models.message.DatasetColumnModel;
import models.message.DatasetColumnSemanticModel;
import models.message.RegisterMessage;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;
import play.mvc.results.Unauthorized;
import play.vfs.VirtualFile;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnCommonMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.layer3.typed.IterableProxy;
import com.alvazan.orm.layer3.typed.IterableReverseProxy;
import com.alvazan.play.NoSql;

import controllers.SecurityUtil;
import controllers.gui.Counter;

public class Utility {

	private static final Logger log = LoggerFactory.getLogger(Utility.class);
	
	private static Random random   = new Random(System.currentTimeMillis());
	private static UniqueKeyGenerator generator = new UniqueKeyGenerator();

	private static ObjectMapper mapper;
	
	/**
	 * Returns a unique id, given a prefix string
	 */
	public static String getUniqueKey() {
		long   randomNum = random.nextLong();
		if(randomNum == Long.MIN_VALUE)
			randomNum ++; //make it so it can switch to positive since negative scale as one more value than positive
		long abs = Math.abs(randomNum);
		String newKey = generator.generateNewKey(null);
		String encodedRandom = Long.toString(abs, 36).toUpperCase();
		String str = newKey+"."+encodedRandom;
		return str.toUpperCase();
	} // getUniqueid

	public static EntityUser getCurrentUserNew(Session session) {
		//FIRST, which user do we use...basic auth OR the form authenticated user
		//In the case of user is logged in and basic auth is supplied directly, we will use basic auth
		//If no basic auth supplied, use form, it both not supplied send back unauthorized
		NoSqlEntityManager mgr = NoSql.em();
		Request r = Request.current();
		if(r.user != null || "".equals(r.user.trim())) {
			//basic auth period!!!!
			EntityUser user = NoSql.em().find(EntityUser.class, r.user);
			if(user == null)
				throw new Unauthorized("Please log into databus before using the api");
			else if(user.getApiKey().equals(r.password))
				return user;
			else if(controllers.gui.Security.authenticate(r.user, r.password))
				return user;
			throw new Unauthorized("User or password is incorrect");
		} else {
			String username = SecurityUtil.getUser();
			if(username == null)
				throw new Unauthorized("You did not supply basic auth credentials nor are you logged into the web application");
		

			EntityUser user = mgr.find(EntityUser.class, username);
			return user;
		}
	} // getCurrentUser
	
	public static EntityUser getCurrentUser(Session session) {
		String username = SecurityUtil.getUser();
		if(username == null)
			return null;
		
		NoSqlEntityManager mgr = NoSql.em();
		EntityUser user = mgr.find(EntityUser.class, username);
		return user;
	} // getCurrentUser
	
	public static void justRegister(String tableJSON, String username) {
		mapper = new ObjectMapper();
		boolean isTimeSeries = true;

		RegisterMessage msg = null;
		try {
			msg = mapper.readValue(tableJSON, RegisterMessage.class);
		} catch (JsonParseException e) {
			if (log.isWarnEnabled())
        		log.warn("There was a problem parsing the JSON request.", e);
		} catch (JsonMappingException e) {
			if (log.isWarnEnabled())
        		log.warn("Your json request appears to be invalid.", e);
		} catch (IOException e) {
			if (log.isWarnEnabled())
        		log.warn("There was an IO error!", e);
		} // try

		if(msg != null) {
			ensureTableDoesNotAlreadyExist(msg);
			EntityUser user = NoSql.em().find(EntityUser.class, username);
			List<SecurityGroup> groupsToAddTableTo = fetchGroupsToAddTableTo(msg, user);

			String postKey = Utility.getUniqueKey();

			// Setup the table meta
			DboTableMeta tm = new DboTableMeta();
			tm.setup(msg.getModelName(), "nreldata", false, null);

			// create new Table here and add to security group as well
			SecureTable t = new SecureTable();
			t.setTableName(msg.getModelName());
			t.setDateCreated(new LocalDateTime());
			t.setTableMeta(tm);
			t.setCreator(user);
			t.setDescription("Please add a description to this table if you are the owner");
			NoSql.em().fillInWithKey(t);

			tm.setForeignKeyToExtensions(t.getId());

			setupEachColumn(msg, t, isTimeSeries);

			NoSqlEntityManager mgr = NoSql.em();

			if (log.isInfoEnabled())
				log.info("table id = '" + tm.getColumnFamily() + "'");

			for (SecurityGroup g : groupsToAddTableTo) {
				if (log.isInfoEnabled())
					log.info("Adding table = '" + t.getTableName() + "' to group = '" + g.getName() + "'");
				g.addTable(t);
				mgr.put(g);
			} // for

			mgr.put(tm);
			mgr.put(t);
			
			mgr.flush();
		} else {
			if (log.isWarnEnabled())
				log.warn("Your table JSON is null or invalid.");			
		}
	}
	private static void ensureTableDoesNotAlreadyExist(RegisterMessage msg) {
		SecureResource t2 = SecureTable.findByName(NoSql.em(), msg.getModelName());
		if(t2 != null) {
			if (log.isInfoEnabled())
				log.info("Sorry, table with tablename='"+msg.getModelName()+"' is already in use");
		} else if (NoSql.em().find(DboTableMeta.class, msg.getModelName()) != null) {
			if (log.isInfoEnabled())
				log.info("Table '" + msg.getModelName() + "' already exists.");
		} // if
	}

	private static EntityUser fetchUser(String key) {
		EntityUser user = EntityUser.findUserByKey(NoSql.em(), key);
		return user;
	}

	private static List<SecurityGroup> fetchGroupsToAddTableTo(
			RegisterMessage msg, EntityUser user) {

		List<RoleMapping> roles = user.getGroups();
		Map<String, SecurityGroup> groupsUserHasAdminRights = formAdminGroups(roles);
		List<SecurityGroup> groupsToAddTableTo = new ArrayList<SecurityGroup>();
		for (String name: msg.getGroups()) {

			SecurityGroup sg = groupsUserHasAdminRights.get(name);

			if(sg == null) {
				if (log.isInfoEnabled())
					log.info("Your user='"+user.getUsername()+"' is not authorized to add tables to group='"+name+"'");
				throw new Unauthorized("Your user id="+user.getUsername()+" is not authorized to add tables to group='"+name+"'");
			} // if

			groupsToAddTableTo.add(sg);

		} // for
		return groupsToAddTableTo;
	}

	private static Map<String, SecurityGroup> formAdminGroups(
			List<RoleMapping> roles) {
		Map<String, SecurityGroup> nameToGroup = new HashMap<String, SecurityGroup>();
		for(RoleMapping r : roles) {
			if(r.getPermission() == PermissionType.ADMIN)
				nameToGroup.put(r.getGroup().getName(), r.getGroup());
		}
		return nameToGroup;
	}

	public static <T> T parseJson(InputStream in, Class<T> type) {
		try {
			return mapper.readValue(in, type);
		} catch (JsonParseException e) {
			if (log.isWarnEnabled())
        		log.warn("There was a problem parsing the JSON request.", e);
		} catch (JsonMappingException e) {
			if (log.isWarnEnabled())
        		log.warn("Your json request appears to be invalid.", e);
		} catch (IOException e) {
			if (log.isWarnEnabled())
        		log.warn("There was an IO error!", e);
		} // try

		throw new IllegalStateException("bug, should never reach here");
	}

	private static void setupEachColumn(RegisterMessage msg, SecureTable t, boolean isTimeSeries) {
		NoSqlEntityManager mgr = NoSql.em();
		DboTableMeta tm = t.getTableMeta();
		// Setup the column meta
		for (DatasetColumnModel c : msg.getColumns()) {

			// Set up the semantic meta for this particular column
			String semanticType = c.getSemanticType();
			SdiColumn col = new SdiColumn();
			col.setColumnName(c.getName());
			col.setSemanticType(semanticType);

			setupSemanticData(mgr, c, col);

			Class dataType = null;

			if (c.getDataType().equalsIgnoreCase("BigDecimal")) {
				dataType = BigDecimal.class;
			} else if (c.getDataType().equalsIgnoreCase("BigInteger")) {
				dataType = BigInteger.class;
			} else if (c.getDataType().equalsIgnoreCase("String")) {
				dataType = String.class;
			} else {
				dataType = byte[].class;
			} // if

			DboColumnMeta colMeta;
			if(c.getIsPrimaryKey()) {
				DboColumnIdMeta cm = formPrimaryKeyMeta(tm, c, dataType, isTimeSeries);
				t.setPrimaryKey(col);
				colMeta = cm;
				if (log.isInfoEnabled())
					log.info("col '" + cm.getColumnName() + "' is a pk " + cm.getId());
			} else {
				DboColumnCommonMeta cm = formCommonColMeta(tm, c, dataType);
				t.getNameToField().put(c.getName(), col);
				colMeta = cm;
				if (log.isInfoEnabled())
					log.info("col '" + cm.getColumnName() + "' is NOT a pk " + cm.getId());
			}

			mgr.put(col);
			mgr.put(colMeta);
		} // for
	}

	private static DboColumnIdMeta formPrimaryKeyMeta(DboTableMeta tm, DatasetColumnModel c, Class dataType, boolean isTimeSeries) {
		if(dataType != BigInteger.class && isTimeSeries) {
			if (log.isInfoEnabled())
				log.info("primary key column was not of type integer yet this is time series data");
		}

		DboColumnIdMeta idMeta = new DboColumnIdMeta();
		idMeta.setup(tm, c.getName(), dataType, true);
		return idMeta;
	}

	private static DboColumnCommonMeta formCommonColMeta(DboTableMeta tm, DatasetColumnModel c, Class dataType) {
		DboColumnCommonMeta cm = new DboColumnCommonMeta();
		cm.setup(tm, c.getName(), dataType, false, false);
		return cm;
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

	public static <T> Iterable<T> fetchProxy(Cursor<T> cursor) {
		Iterable<T> proxy = new IterableProxy<T>(cursor);
		String reverseParam = play.mvc.Http.Request.current().params.get("reverse");
		if("true".equalsIgnoreCase(reverseParam))
			proxy = new IterableReverseProxy<T>(cursor);
		return proxy;
	}
	
	public static String getSolrServer() {
		String prop = Play.configuration.getProperty("solr.url");

		if(prop == null)
			throw new IllegalArgumentException("must supply solr.url property");
			
		return prop;		
	}
	
	public static String getRedirectProtocol() {
		String mode = play.Play.configuration.getProperty("application.mode");
		String demomode = play.Play.configuration.getProperty("demo.mode");
		   
		String protocol = "http";
		if("prod".equals(mode) && !"true".equals(demomode)) {
		   	protocol = "https";
		}
		
		return protocol;
	}

	public static boolean isDemoMode() {
		String property = Play.configuration.getProperty("demo.mode");
		if(property == null)
			return false;
		else if("true".equals(property.trim()))
			return true;
		return false;
	}

	public static boolean isDisplayHighChartsBanner() {
		String property = Play.configuration.getProperty("gui.charting.library");
		if("highcharts_licensed".equals(property) || "highcharts_disabled".equals(property))
			return false;
		return true;
	}

	public static String getVersion() {
		VirtualFile verFile = Play.getVirtualFile("/version.txt");
		if(verFile == null) {
			log.warn("version file=/version.txt is missing");
			return "";
		}
		String version = verFile.contentAsString();
		return version;
	}
	
	public static String createCfName(String modelName) {
		int hashCode = modelName.hashCode();
		int bucketNumber = Math.abs(hashCode % 10);
		if(bucketNumber > 9 || bucketNumber < 0)
			throw new RuntimeException("bug, bucket number should be 0 to 9....hashCode="+hashCode+" buckNum="+bucketNumber);
		String realCf = "nreldata"+bucketNumber;
		return realCf;
	}

	public static void removeKeyToTableRows(SecureSchema schema, Set<EntityUser> users, Counter c) {
		CursorToMany<SecureTable> cursor = schema.getTablesCursor();
		while(cursor.next()) {
			SecureTable t = cursor.getCurrent();
			for(EntityUser user : users) {
				String key = KeyToTableName.formKey(t.getName(), user.getUsername(), user.getApiKey());
				KeyToTableName ref = NoSql.em().getReference(KeyToTableName.class, key);
				NoSql.em().remove(ref);
				
				if(c.getCount() % 100 == 0)
					NoSql.em().flush();
			}
		}

		NoSql.em().flush();
	}

	public static Set<EntityUser> findAllUsers(Entity entity) {
		Set<EntityUser> users = new HashSet<EntityUser>();
		if(entity instanceof EntityUser) {
			users.add((EntityUser) entity);
		} else {
			addUsersToList((EntityGroup) entity, users);
		}
		return users;
	}
	
	public static void addUsersToList(EntityGroup grp, Set<EntityUser> users) {
		for(EntityGroupXref ent : grp.getChildren()) {
			Entity entity = ent.getEntity();
			if(entity instanceof EntityGroup)
				addUsersToList((EntityGroup) entity, users);
			else
				users.add((EntityUser) entity);
		}
	}
} // Utility
