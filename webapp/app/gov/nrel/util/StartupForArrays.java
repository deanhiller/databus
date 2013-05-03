package gov.nrel.util;

import models.Entity;
import models.PermissionType;
import models.RoleMapping;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;

public class StartupForArrays {

	private static String[] tables = { "{\"datasetType\":\"STREAM\",\n \"modelName\":\"stream1\",\n \"groups\":[\"important\"],\n \"columns\":[\n      {\"name\":\"idName\",\"dataType\":\"BigInteger\",\"semanticType\":\"oei:timestamp\",\"isIndex\":false,\"isPrimaryKey\":true,\"semantics\":[]},\n      {\"name\":\"color\",\"dataType\":\"string\",\"semanticType\":\"oei:color\",\"isIndex\":false,\"isPrimaryKey\":false,\"semantics\":[]},\n      {\"name\":\"volume\",\"dataType\":\"BigDecimal\",\"semanticType\":\"oei:volume\",\"isIndex\":false,\"isPrimaryKey\":false,\"semantics\":[]}\n      ]\n}\n", 
	"{\"datasetType\":\"STREAM\",\n \"modelName\":\"stream2\",\n \"groups\":[\"notImportant\"],\n \"columns\":[\n      {\"name\":\"idName\",\"dataType\":\"BigInteger\",\"semanticType\":\"oei:timestamp\",\"isIndex\":true,\"isPrimaryKey\":true,\"semantics\":[]},\n      {\"name\":\"color\",\"dataType\":\"string\",\"semanticType\":\"oei:color\",\"isIndex\":false,\"isPrimaryKey\":false,\"semantics\":[]},\n      {\"name\":\"volume\",\"dataType\":\"BigDecimal\",\"semanticType\":\"oei:volume\",\"isIndex\":false,\"isPrimaryKey\":false,\"semantics\":[]}\n      ]\n}\n" };
	public static String[] users = { "bill", "bob" };
	private static String[] groups = { "important", "notImportant" };
	private static String[] roleMappings = { "admin:bill:important",
			"admin:bob:notImportant", "readwrite:bill:notImportant" };
	
	static void createStuff(NoSqlEntityManager em) {
		// create users
		for (int i = 0; i < users.length; i++) {
			createUser(em, users[i]);
		}

		// create groups
		for (int i = 0; i < groups.length; i++) {
			createGroup(em, groups[i]);
		}

		// create role mappings between users and groups
		for (int i = 0; i < roleMappings.length; i++) {
			createRoleMapping(em, roleMappings[i]);
		}
		
		// create tables
		for (int i = 0; i < tables.length; i++) {
			createTable(em, tables[i], users[i]);
		}

		// create table maps to security groups
		/*for (int i = 0; i < tableMappings.length; i++) {
			createTableMapping(em, tableMappings[i]);
		}*/
	}

	private static void createTable(NoSqlEntityManager mgr, String tableMsg, String username) {
		/*SdiTable table = new SdiTable();
		table.setTableName(tableName);
		mgr.put(table);
		mgr.flush();*/
		Utility.justRegister(tableMsg, username);
	}

	private static void createUser(NoSqlEntityManager mgr, String username) {
		EntityUser user = EntityUser.create(username);
		mgr.put(user);
		mgr.flush();
	}

	private static void createGroup(NoSqlEntityManager mgr, String groupName) {
		SecurityGroup securityGroup = new SecurityGroup();
		securityGroup.setName(groupName);
		mgr.put(securityGroup);
		mgr.flush();
	}

	private static void createTableMapping(NoSqlEntityManager mgr,
			String mapping) {
		String[] mapElements = mapping.split(":");
		String tableName = mapElements[0];
		String groupName = mapElements[1];

		SecureTable table = SecureTable.findByName(mgr, tableName);
		SecurityGroup group = SecurityGroup.findByName(mgr, groupName);

		group.addTable(table);

		mgr.put(table);
		mgr.put(group);

		mgr.flush();
	}

	private static void createRoleMapping(NoSqlEntityManager mgr, String mapping) {
		String[] mapElements = mapping.split(":");
		String permissionName = mapElements[0];
		String userName = mapElements[1];
		String groupName = mapElements[2];

		PermissionType permission = PermissionType.lookup(permissionName);
		SecurityGroup group = SecurityGroup.findByName(mgr, groupName);
		EntityUser user = mgr.find(EntityUser.class, userName);

		RoleMapping roleMap = new RoleMapping(user, group, permission);
		mgr.put(roleMap);
		mgr.put(user);
		mgr.put(group);

		mgr.flush();
	}
}
