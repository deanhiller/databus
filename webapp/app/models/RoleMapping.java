package models;

import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;

@NoSqlEntity
public class RoleMapping {

	@NoSqlId
	private String id;
	
	@NoSqlManyToOne
	private EntityUser user;
	@NoSqlManyToOne
	private SecurityGroup group;
	
	private String permission;
	
	public RoleMapping() {
	}
	
	public RoleMapping(EntityUser user, SecurityGroup group, PermissionType p) {
		if(user == null || group == null)
			throw new IllegalArgumentException("Can't pass in null user or null group");
		user.addGroup(this);
		group.addUser(this);
		this.user = user;
		this.group = group;
		this.permission = p.value();
	}

	public String getGroupName() {
		if(group == null)
			return null;
		return group.getName();
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public EntityUser getUser() {
		return user;
	}
	public void setUser(EntityUser user) {
		this.user = user;
	}
	public SecurityGroup getGroup() {
		return group;
	}
	public void setGroup(SecurityGroup group) {
		this.group = group;
	}
	
	public void setPermission(PermissionType p) {
		this.permission = p.value();
	}
	public PermissionType getPermission() {
		return PermissionType.lookup(permission);
	}
	public boolean isAdminRole() {
		return getPermission() == PermissionType.ADMIN;
	}

	public boolean isHigherRoleThan(RoleMapping mapping) {
		if(getPermission().isHigherRoleThan(mapping.getPermission()))
			return true;
		return false;
	}
}
