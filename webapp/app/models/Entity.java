package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlInheritance;
import com.alvazan.orm.api.base.anno.NoSqlInheritanceType;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;

@NoSqlEntity(columnfamily="User")
@NoSqlInheritance(discriminatorColumnName="classType", strategy=NoSqlInheritanceType.SINGLE_TABLE,
    subclassesToScan={EntityUser.class, EntityGroup.class} )
public abstract class Entity {

	private static final Logger log = LoggerFactory.getLogger(Entity.class);
	
	@NoSqlId(usegenerator=false, columnName="username")
	protected String id;
	
	private String classType;
	
	@NoSqlOneToMany
	private List<EntityGroupXref> parentGroups = new ArrayList<EntityGroupXref>();
	
	@NoSqlOneToMany
	private List<SecureResourceGroupXref> resources = new ArrayList<SecureResourceGroupXref>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public abstract String getName();
	public abstract String getTypeString();
	public abstract UserType getType();
	
	public String getClassType() {
		return classType;
	}
	
	public void setClassType(String classType) {
		this.classType = classType;
	}
	
	public List<EntityGroupXref> getParentGroups() {
		return parentGroups;
	}

	protected void addSecureEntity(SecureResourceGroupXref entity) {
		resources.add(entity);
	}
	
	public List<SecureResourceGroupXref> getResources() {
		return resources;
	}
	
	public List<SecureResourceGroupXref> getSchemas() {
		List<SecureResourceGroupXref> result = new ArrayList<SecureResourceGroupXref>();
		for (SecureResourceGroupXref xref:getResources()) {
			if (xref.getResource() instanceof SecureSchema)
				result.add(xref);
		}
		return result;
	}
	protected void addParentGroup(EntityGroupXref entityGroupXref) {
		parentGroups.add(entityGroupXref);
	}
	
	public void addResources(Set<PermissionType> roles, SecureResource resource) {
		for(SecureResourceGroupXref ref : getResources()) {
			if(log.isDebugEnabled()) 
				log.debug("checking on xref="+ref.getId()+" to see if match id="+resource.getId()+" which we add role="+ref.getPermission());
			if(ref.getResource().getId().equals(resource.getId()))
				roles.add(ref.getPermission());
		}
		
		for(EntityGroupXref xref : getParentGroups()) {
			EntityGroup group = xref.getGroup();
			if(log.isDebugEnabled())
				log.debug("adding roles from parent="+group.getName()+" id="+group.getId());
			group.addResources(roles, resource);
		}
	}
}
