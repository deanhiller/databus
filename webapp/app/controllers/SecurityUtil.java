package controllers;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;
import play.mvc.Http;
import play.mvc.Util;
import play.mvc.results.Unauthorized;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.PermissionType;
import models.RoleMapping;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;
import models.EntityUser;

import com.alvazan.play.NoSql;

import controllers.auth.ActiveDirAuthentication;

public class SecurityUtil {

	private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);

//	@Util
//	public static void check(String postKey, SecureSchema schema) {
//		Request request = Request.current();
//		String username = request.user;
//		String password = request.password;
//		
//		if(username == null) {
//			SecurityGroup group = SecurityGroup.findByName(NoSql.em(), schema.getName());
//			//old way
//			if(!group.getApiKeyForPost().equals(postKey)) {
//				if (log.isInfoEnabled())
//					log.info("key not valid for group="+schema.getName()+" groupkey="+group.getApiKeyForPost()+" your key="+postKey);
//				throw new Unauthorized("Group does not exist OR the key is not valid for this group(server logs say which)");
//			}
//			return;
//		}
//
//		EntityUser user = EntityUser.findByName(NoSql.em(), username);
//		if(user == null || !user.getApiKey().equals(password)) {
//			if (log.isInfoEnabled())
//				log.info("user or key not valid for user="+user);
//			throw new Unauthorized("user or key not valid for user="+user);
//		}
//		
//		Map<String, SecureResourceGroupXref> schemas = new HashMap<String, SecureResourceGroupXref>();
//		addGroups(user, schemas );
//		if(schemas.get(schema.getId()) == null) {
//			if (log.isInfoEnabled())
//				log.info("user="+user.getName()+" has no access to schema="+schema.getName());
//			throw new Unauthorized("user="+user.getName()+" has no access to schema="+schema.getName());			
//		}
//	}
	
	@Util
	public static Map<String, SecureResourceGroupXref> groupSecurityCheck() {
		Session session = Session.current();
		EntityUser user = Utility.getCurrentUser(session);
		
		Request request = Request.current();
		return getGroupSecurityCheck(user, request);
	}
	
	@Util
	public static Map<String, SecureResourceGroupXref> getGroupSecurityCheck(EntityUser user, Request request) {
		Map<String, SecureResourceGroupXref> schemaIds = new HashMap<String, SecureResourceGroupXref>();

		String username = request.user;
		String key = request.password;

		if(user != null) {
			if (log.isInfoEnabled())
				log.info("using form authentication user="+user.getUsername());
			addGroups(user, schemaIds);
		} else if("program".equals(username)) {
			if (log.isInfoEnabled())
				log.info("program user is deprecated.  Create and use a robot instead now");
			throw new Unauthorized("program user is deprecated.  Create and use a robot instead now");
		} else if(StringUtils.isBlank(username)) {
			if (log.isInfoEnabled())
				log.info("no username provided, throwing Unauthorized");
			throw new Unauthorized("Must supply basic auth username and key(key goes in password field)");
		} else {
			if (log.isInfoEnabled())
				log.info("using basic auth of username="+username);
			user = NoSql.em().find(EntityUser.class, username);
			if(!username.startsWith("robot")) {
				if(!user.getApiKey().equals(key)) 
					authenticateLdapUser(username, key);
			} else {
				authenticateRobot(user, username, key);
			}
			
			addGroups(user, schemaIds);
		}
		
		return schemaIds;
	}


	private static void authenticateRobot(EntityUser user, String username,
			String key) {
		if(user == null || !user.getApiKey().equals(key)) {
			if (log.isInfoEnabled())
				log.info("key is invalid for user="+username);
			throw new Unauthorized("key is invalid for user="+username+" or username does not exist");
		}
	}

	private static void authenticateLdapUser(String username, String key) {
		boolean userValid = Security.authenticate(username, key);
		if(!userValid) {
			if (log.isInfoEnabled())
				log.info("key is invalid for user="+username);
			throw new Unauthorized("key is invalid for user="+username+" or username does not exist");					
		}
	}

	private static void addGroups(EntityUser user, Map<String, SecureResourceGroupXref> schemaIds) {
		List<EntityGroupXref> refs = user.getParentGroups();
		if (log.isInfoEnabled())
			log.info("user not admin="+user.getUsername());
		for(EntityGroupXref ref : refs) {
			fillInSchemas(ref.getGroup(), schemaIds);
		}
		List<SecureResourceGroupXref> resources = user.getResources();
		for (SecureResourceGroupXref ref : resources) {
			addToMap(schemaIds, ref);
		}
	}

	private static void fillInSchemas(EntityGroup group,
			Map<String, SecureResourceGroupXref> schemaIds) {
		List<SecureResourceGroupXref> resRefs = group.getResources();
		for(SecureResourceGroupXref resRef : resRefs) {
			addToMap(schemaIds, resRef);
		}
	}

	private static void addToMap(
			Map<String, SecureResourceGroupXref> schemaIds, SecureResourceGroupXref resRef) {
		SecureResource res = resRef.getResource();
		if(!(res instanceof SecureSchema))
			return;
		
		SecureSchema schema = (SecureSchema) res;
		SecureResourceGroupXref xref = schemaIds.get(schema.getId());
		if(xref == null) {
			schemaIds.put(schema.getId(), resRef);
			return;
		}

		if(xref.isHigherRoleThan(resRef))
			return; //nothing to do
		
		//replace as this one is a higher role
		schemaIds.put(schema.getId(), resRef);
	}

	@Util
	public static SecureTable checkTable(Map<String, SecureResourceGroupXref> schemaIds, String cf) {
		Request request = Request.current();
		
		SecureTable sdiTable = SecureTable.findByName(NoSql.em(), cf);
		if(isAdmin())
			return sdiTable;
		
		String username = request.user;
		String key = request.password;
		if(sdiTable == null) {
			if (log.isInfoEnabled())
				log.info("table="+cf+" does not exist or is not in meta.  user="+username+" key="+key);
			throw new Unauthorized("Either table does not exist OR you are unauthorized to use this table="+cf);
		} else if(schemaIds.get(sdiTable.getSchema().getId()) == null) {
			if (log.isInfoEnabled())
				log.info("table="+cf+" is not accessible from this user="+username+" key="+key);
			throw new Unauthorized("Either table does not exist OR you are unauthorized to use this table="+cf);
		}
		return sdiTable;
	}

	@Util
	public static PermissionType checkSingleTable2(String cf) {
		Map<String, SecureResourceGroupXref> schemaIds = groupSecurityCheck();
		Request request = Request.current();
		
		SecureTable sdiTable = SecureTable.findByName(NoSql.em(), cf);
		if(isAdmin())
			return PermissionType.ADMIN;
		
		String username = request.user;
		String key = request.password;
		if(sdiTable == null) {
			if (log.isInfoEnabled())
				log.info("table="+cf+" does not exist or is not in meta.  user="+username+" key="+key);
			throw new Unauthorized("Either table does not exist OR you are unauthorized to use this table="+cf);
		} 
		
		SecureResourceGroupXref xref = schemaIds.get(sdiTable.getSchema().getId());
		if(xref == null) {
			if (log.isInfoEnabled())
				log.info("table="+cf+" is not accessible from this user="+username+" key="+key);
			throw new Unauthorized("Either table does not exist OR you are unauthorized to use this table="+cf);
		}
		return xref.getPermission();
	}
	
	@Util
	public static SecureTable checkSingleTable(String colFamily) {
		Map<String, SecureResourceGroupXref> schemaIds = groupSecurityCheck();
		return checkTable(schemaIds, colFamily);
	}
	
	@Util
	public static SecureTable checkSingleTable(String colFamily, EntityUser user, Request request) {
		Map<String, SecureResourceGroupXref> schemaIds = getGroupSecurityCheck(user, request);
		return checkTable(schemaIds, colFamily);
	}

	@Util
	public static EntityUser fetchUser(String username, String apiKey) {
		EntityUser user = EntityUser.findByName(NoSql.em(), username);
		if(user == null) {
			throw new Unauthorized("User="+username+" is not authorized");
		} else if(!user.getApiKey().equals(apiKey)) {
			if (log.isInfoEnabled())
				log.info("user or key is invalid for user="+username+" key="+apiKey);
			throw new Unauthorized("user or key is invalid for user="+username);
		}
		
		return user;
	}

	public static boolean hasRoleOrHigher(String schemaId, PermissionType permission) {
		Map<String, SecureResourceGroupXref> schemaIds = SecurityUtil.groupSecurityCheck();
		SecureResourceGroupXref xref = schemaIds.get(schemaId);
		if(isAdmin())
			return true;
		else if(xref == null)
			return false;
		else if(permission.isHigherRoleThan(xref.getPermission()))
				return false;
		return true;
	}

	public static boolean isAdmin() {
		Session session = Session.current();
		EntityUser user = Utility.getCurrentUser(session);
		if(user != null && user.isAdmin())
			return true;
		if (StringUtils.isNotBlank(Request.current().user)) {
			EntityUser newUser = NoSql.em().find(EntityUser.class, Request.current().user);
			if(newUser.isAdmin())
				return true;
		}

		return false;
	}
	
}
