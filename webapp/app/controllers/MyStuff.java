package controllers;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.KeyToTableName;
import models.PermissionType;
import models.RoleMapping;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Http.Request;
import play.mvc.results.NotFound;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;

import controllers.auth.Secure;

@With(Secure.class)
public class MyStuff extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyStuff.class);
	
	@Deprecated
	public static void myGroupsOld() {
		EntityUser user = Utility.getCurrentUser(session);

		List<RoleMapping> groups = user.getGroups();
		if (log.isInfoEnabled()) {
			for (RoleMapping m : groups) {
				log.info("mapping=" + m.getGroup());
			}
		}

		render(user);
	}

	
	public static void myTables() {
		EntityUser user = Utility.getCurrentUser(session);
		Map<String, TableInfo> loadedTables = new HashMap<String, TableInfo>();
		
		int counter = 0;
		List<RoleMapping> groups = user.getGroups();
		for(RoleMapping r : groups) {
			SecurityGroup group = r.getGroup();
			if (log.isInfoEnabled())
				log.info("checking group="+group.getName());
			CursorToMany<SecureTable> tables = group.getTables();
			while(tables.next() && counter < 50) {
				SecureTable t = tables.getCurrent();
				if (log.isInfoEnabled())
					log.info("checking table="+t.getTableName());
				TableInfo info = loadedTables.get(t.getId());
				if(info == null) {
					if (log.isInfoEnabled())
						log.info("no other relationship with table="+t.getTableName()+", adding");
					loadedTables.put(t.getId(), new TableInfo(t, group, r));
					counter++;
				} else {
					if (log.isInfoEnabled())
						log.info("already related to table though another group..adding group="+group.getId());
					info.setHighestMapping(r);
					info.addGroup(group);
				}
			}
		}

		Collection<TableInfo> infos = loadedTables.values();
		render(infos);
	}
	
	
	public static void editGroupOld(String group) {
		EntityUser user = Utility.getCurrentUser(session);

		SecurityGroup groupDbo = new SecurityGroup();
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (group != null) {
			groupDbo = securityGroupCheck(group, user);
			tables = groupDbo.getTables(0, 50);
		}
		
		String oldGroupName = groupDbo.getName();
		render(user, groupDbo, oldGroupName, tables);
	}
	
	
	private static SecurityGroup securityGroupCheck(String group, EntityUser user) {
		if(user.isAdmin()) {
			SecurityGroup theGroup = SecurityGroup.findByName(NoSql.em(), group);
			if(theGroup == null)
				notFound("group not found");
			return theGroup;
		}
		// if group is not null, let's check if user has access to it...
		List<RoleMapping> groups = user.getGroups();
		for (RoleMapping r : groups) {
			SecurityGroup groupDbo = r.getGroup();
			String name = groupDbo.getName();
			if (name.equals(group)) {
				if (r.getPermission() == PermissionType.ADMIN) {
					return r.getGroup();
				}
				break;
			}
		}

		notFound("You either don't have access or the group does not exist");
		return null; // This won't happen since notFound throws Exception
	}

	
	private static SecureSchema authTableCheck(String table, EntityUser user) {
		NoSqlEntityManager em = NoSql.em();
		SecureTable sdiTable = SecureTable.findByName(em, table);
		if(sdiTable == null)
			notFound();
		
		String rowKey = KeyToTableName.formKey(table, user.getUsername(), user.getApiKey());
		KeyToTableName info = em.find(KeyToTableName.class, rowKey);
		if (info != null || user.isAdmin()) {
			return sdiTable.getSchema();
		}
		
		return null;
	}
	

//	private static SecurityGroup securityGroupsCheck(
//			List<SecurityGroup> groups, User user) {
//		// if group is not null, let's check if user has access to it...
//		for (SecurityGroup group : groups) {
//			List<RoleMapping> roleMaps = group.getUsers();
//			for (RoleMapping r : roleMaps) {
//				User userInGroup = r.getUser();
//				if (user.getUsername().equals(userInGroup.getUsername())) {
//					if (r.getPermission() == PermissionType.ADMIN) {
//						return r.getGroup();
//					}
//					break;
//				}
//			}
//		}
//
//		notFound("You either don't have access or the group does not exist");
//		return null; // This won't happen since notFound throws Exception
//	}

	
	
//	public static void myTables(String group) {
//		NoSqlEntityManager mgr = NoSql.em();
//		SecurityGroup groupDbo = SecurityGroup.findByName(mgr, group);
//
//		List<SdiTable> tables = groupDbo.getTables();
//		for (SdiTable t : tables) {
//			log.info("mapping=" + t.getTableName());
//		}
//
//		render(tables);
//	}
	
	public static void tableAdd(String group) {
		EntityUser user = Utility.getCurrentUser(session);
		securityGroupCheck(group, user);
		
		SecureResource table = new SecureTable();
		render(group, table);
	}

	public static void tableEdit(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		
		if (targetTable == null) {
			notFound("Page not found");
		}
		if (log.isInfoEnabled())
			log.info("loaded the table "+targetTable.getName()+" searchable="+targetTable.isSearchable());
		
		render(group, targetTable);
	}
	
	public static void tableData(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		String username = user.getUsername();
		String password = user.getApiKey();
		String host = Request.current().host;		
		String baseurl = "//"+host;
		
		SecureResource targetTable = SecureTable.findByName(NoSql.em(), table);
		if (targetTable == null) {
			notFound("Page not found");
		}
		
		render(username, password, baseurl, targetTable);
	}
	
	public static void aggData(String aggregationName) {
		EntityUser user = Utility.getCurrentUser(session);

		String username = user.getUsername();
		String password = user.getApiKey();
		String host = Request.current().host;		
		String baseurl = "//"+host;
		
		//check to make sure this agg actually exists
//		SecureResource targetTable = SecureTable.findByName(NoSql.em(), table);
//		if (targetTable == null) {
//			notFound("Page not found");
//		}
		String agg = aggregationName;
		render(username, password, baseurl, agg);
	}

	public static void postTable(String group, SecureResource targetTable) {
		badRequest("We need to implement this");
	}
	
}
