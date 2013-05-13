package models;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDateTime;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;

@NoSqlDiscriminatorColumn("userImpl")
@NoSqlQueries({
	@NoSqlQuery(name="findAll", query="select u from TABLE as u"),
	@NoSqlQuery(name="findByName", query="select u from TABLE as u where u.username = :username"),
	@NoSqlQuery(name="findByKey", query="select u from TABLE as u where u.apiKey = :apiKey")
})
public class EntityUser extends Entity {
	 
	@NoSqlIndexed
	private String apiKey;
	
	private String classType = "userImpl";
	
	@NoSqlEmbedded
	private UserSettings userSettings = new UserSettings();
	
	private boolean isAdmin;
	
	/**
	 * "robot" or "user"
	 */
	private String type;
	
	// Need to add converter to ORM for LocalDateTime
	private transient LocalDateTime lastLoggedIn; 
	
	@NoSqlOneToMany(keyFieldForMap="name")
	private List<RoleMapping> securityGroups = new ArrayList<RoleMapping>();
	
	//NOTE: This is here as a work around OR create user/create robot throw an exception that doesn't affect behavior but is really
	//annoying.  We should change the webpage to perhaps pass in the username itself instead of using the EntityUser???
	private transient String username;

	private String password;
	
	public String getUsername() {
		return id;
	} // getUsername

	public void setUsername(String username) {
		this.id = username;
	} // setUsername
	
	public String getName(){
		return id;
	}
	
	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public boolean isAdmin() {
		return isAdmin;
	} // isAdmin

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	} // setAdmin

	public LocalDateTime getLastLoggedIn() {
		return lastLoggedIn;
	} // getLastLoggedIn

	public void setLastLoggedIn(LocalDateTime lastLoggedIn) {
		this.lastLoggedIn = lastLoggedIn;
	} // setLastLoggedIn

	void addGroup(RoleMapping tableSide) {
		securityGroups.add(tableSide);
	} // addMapping

	public List<RoleMapping> getGroups() {
		return securityGroups;
	} // getSecurityGroups
	
	public String getApiKey() {
		return apiKey;
	} // getApiKey
	
	public void setApiKey(String apiKeyString) {
		this.apiKey = apiKeyString;
	} // setApiKey
	
	
	public static EntityUser findUserByKey(NoSqlEntityManager mgr, String key) {
		Query<EntityUser> query = mgr.createNamedQuery(EntityUser.class, "findByKey");
		query.setParameter("apiKey", key);
		return query.getSingleObject();
	}

	public static List<EntityUser> findAll(NoSqlEntityManager mgr) {
		Query<EntityUser> query = mgr.createNamedQuery(EntityUser.class, "findAll");
		Cursor<KeyValue<EntityUser>> cursor = query.getResults("apiKey");
		List<EntityUser> users = new ArrayList<EntityUser>();
		while(cursor.next()) {
			KeyValue<EntityUser> kv = cursor.getCurrent();
			if(kv.getValue() == null)
				continue;
			users.add(kv.getValue());
		}
		return users;
	}
	
	public static EntityUser create(String username) {
		EntityUser user = new EntityUser();
		user.setUsername(username);
		user.setApiKey(Utility.getUniqueKey());
		user.setLastLoggedIn(new LocalDateTime());
		return user;
	}

	@Override
	public String toString() {
		return getUsername();
	}

	public List<EntityUser> getRobots() {
		Set<String> processedIds = new HashSet<String>();
		List<EntityGroupXref> groups = getParentGroups();
		List<EntityUser> robots = new ArrayList<EntityUser>();
		for(EntityGroupXref ref : groups) {
			ref.getRobots(processedIds, robots);
		}
		return robots;
	}
	
	public SecurityGroup findAdminGroup(SecureTable table) {
		List<SecurityGroup> groups = table.getOwningGroups();
		// if group is not null, let's check if user has access to it...
		for (SecurityGroup group : groups) {
			if(isAdminInGroup(group))
				return group;
		}
		return null;
	}
	
	public boolean isAdminInGroup(SecurityGroup g) {
		if(isAdmin)
			return true;
		
		for(RoleMapping m : this.getGroups()) {
			if(m.getGroup().getId().equals(g.getId())
					&& m.isAdminRole())
					return true;
		}
		return false;
	}

	public boolean isAuthorizedForRead(SecureTable sdiTable) {
		if(isAdmin)
			return true;

		List<SecurityGroup> owningGroups = sdiTable.getOwningGroups();
		for(RoleMapping m : this.getGroups()) {
			SecurityGroup grp = m.getGroup();
			if(owningGroups.contains(grp))
				return true;
		}
		return false;
	}
	
	public static EntityUser findByName(NoSqlEntityManager mgr, String userName) {
		Query<EntityUser> query = mgr.createNamedQuery(EntityUser.class, "findByName");
		query.setParameter("username", userName);
		return query.getSingleObject();
	}
	
	public UserType getType() {
		return UserType.lookup(type);
	}
	
	public void setType(UserType type) {
		this.type = type.getDbValue();
	}
	
	public String getTypeString() {
		if(getType() == UserType.USER)
			return "User";
		return "Robot";
	}
	
	public String getRobotGroupName() {
		if(getType() == UserType.ROBOT) {
			List<EntityGroupXref> groups = getParentGroups();
			if(groups.size() == 1) {
				return groups.get(0).getGroup().getName();
			}
		}
		
		return "";
	}

	public void setPassword(String pw) {
		this.password = pw;
	}

	public String getPassword() {
		return password;
	}

	public UserSettings getUserSettings() {
		//avoid null pointer exceptions
		if(userSettings == null)
			userSettings = new UserSettings();
		return userSettings;
	}

	public void setUserSettings(UserSettings userSettings) {
		this.userSettings = userSettings;
	}
	
	
	
} // User
