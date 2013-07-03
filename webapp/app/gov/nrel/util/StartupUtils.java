package gov.nrel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.PermissionType;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.play.NoSql;

import controllers.api.ApiPostDataPointsImpl;
import controllers.api.ApiRegistrationImpl;

/**
 * Compilation of commonly used Startup methods
 * 
 * @author prusso
 *
 */
public class StartupUtils {
	
	/**
	 *  ----------------------------------
	 *  ------- Creating DB Resources -------
	 *  ----------------------------------
	 */
	public static EntityUser createEntityUser(NoSqlEntityManager em, String username, String apikey, Boolean isAdmin ) {
		EntityUser user = new EntityUser();
		user.setUsername(username);
		user.setAdmin(true);
		user.setApiKey(apikey);
		
		em.put(user);
		
		return user;
	}
	
	public static EntityGroup createEntityGroup(NoSqlEntityManager em,
			EntityUser adminUser, String entitygroupName, EntityGroup parentGroup, String... userNames) {
		EntityGroup e = new EntityGroup();
		e.setName(entitygroupName);
		String id = UniqueKeyGenerator.generateKey();
		e.setId(id);
		
		EntityGroupXref usergroupxref = new EntityGroupXref(e, adminUser, true, false);
		em.put(usergroupxref);
		if (parentGroup != null) {
			EntityGroupXref parentGrouptoGroupxref = new EntityGroupXref(parentGroup, e, true, false);
			em.put(parentGrouptoGroupxref);
		}
		
		for (String userName : userNames) {
			EntityUser foundUser = EntityUser.findByName(em, userName);
			if (foundUser == null) {
				foundUser = new EntityUser();
				foundUser.setApiKey("somerandomkey"+userName);
				foundUser.setUsername(userName);
				em.fillInWithKey(foundUser);
				
				NoSql.em().put(foundUser);
			}
			usergroupxref = new EntityGroupXref(e, foundUser, false, false);
			//put it each time
			em.fillInWithKey(usergroupxref);
			em.put(usergroupxref);
			em.put(foundUser);
			
		}
		em.put(adminUser);
		em.put(e);
		
		return e;
	}
	
	public static void createDatabase(NoSqlEntityManager mgr, EntityUser adminUser, String dbName, Entity... users) {
		SecureSchema schema = new SecureSchema();
		schema.setCreator(adminUser);
		schema.setSchemaName(dbName);
		
		schema.setDescription("This would be some very long boring" +
				" description of what this group is for, why it exists and" +
				" blah blah, if you are still reading this, man, you are such a geek," +
				" but I need to take up space so I wouldn't expect you to keep reading" +
				" this...still reading this?  what are you crazy, go get some real work done");
		mgr.put(schema);
		
		for (Entity e : users) {
			SecureResourceGroupXref xref = new SecureResourceGroupXref(e, schema, PermissionType.READ_WRITE);
			mgr.put(xref);
			mgr.put(e);
		}	
		
		SecureResourceGroupXref xref = new SecureResourceGroupXref(adminUser, schema, PermissionType.ADMIN);
		mgr.put(xref);
		mgr.put(adminUser);

		mgr.put(schema);
	}
	
	public static void createTimeSeriesTable(String tableName, String databaseName, EntityUser user) {
		RegisterMessage msg = new RegisterMessage();
		msg.setDatasetType(DatasetType.STREAM);
		msg.setSchema(databaseName);
		msg.setModelName(tableName);
		
		List<DatasetColumnModel> cols = new ArrayList<DatasetColumnModel>();
		StartupUtils.createColumn(cols, "time", "BigInteger", "oei:timestamp", true, true);
		StartupUtils.createColumn(cols, "value", "BigDecimal", "oei:temparaturecolor", false, false);
		msg.setColumns(cols);	

		//register the time series table we will use
		ApiRegistrationImpl.registerImpl(msg, user.getUsername(), user.getApiKey());
	}
	
	public static void createColumn(List<DatasetColumnModel> cols, String name,
			String dataType, String semanticType, boolean isIndex, boolean isPrimaryKey) {
		DatasetColumnModel col = new DatasetColumnModel();
		col.setName(name);
		col.setDataType(dataType);
		col.setSemanticType(semanticType);
		col.setIsIndex(isIndex);
		col.setIsPrimaryKey(isPrimaryKey);
		cols.add(col);
	}
	
	/**
	 *  ----------------------------------
	 *  ----- Manipulating DB Resources -----
	 *  ----------------------------------
	 */
	public static void putTimeSeriesData(String tableName, String time, String value, EntityUser user) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put("_tableName", tableName);
		json.put("time", time);
		json.put("value", value);
		ApiPostDataPointsImpl.postDataImpl(null, json, user.getUsername(), user.getApiKey());
	}
}
