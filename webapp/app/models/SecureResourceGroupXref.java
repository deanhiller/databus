package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alvazan.orm.api.base.ToOneProvider;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;

@NoSqlEntity
public class SecureResourceGroupXref {

	@NoSqlId
	private String id;
	
	@NoSqlManyToOne
	private ToOneProvider<SecureResource> resource = new ToOneProvider<SecureResource>();
	
	@NoSqlManyToOne
	private ToOneProvider<Entity> userOrGroups = new ToOneProvider<Entity>();

	private String permission;
	
	public SecureResourceGroupXref(Entity userOrGroup, SecureResource secureEntity, PermissionType p) {
		if(userOrGroup == null || secureEntity == null || p == null)
			throw new IllegalArgumentException("Can't pass in ANY null parameters");
		userOrGroup.addSecureEntity(this);
		secureEntity.addUser(this);
		setUserOrGroup(userOrGroup);
		setResource(secureEntity);
		this.permission = p.value();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
	public SecureResource getResource() {
		return resource.get();
	}

	public void setResource(SecureResource user) {
		resource.set(user);
	}

	public Entity getUserOrGroup() {
		return this.userOrGroups.get();
	}

	public void setUserOrGroup(Entity user) {
		this.userOrGroups.set(user);
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

	public boolean isHigherRoleThan(SecureResourceGroupXref ref) {
		if(getPermission().isHigherRoleThan(ref.getPermission()))
			return true;
		return false;
	}
	
	public boolean isHigherRoleThan(RoleMapping mapping) {
		if(getPermission().isHigherRoleThan(mapping.getPermission()))
			return true;
		return false;
	}

}
