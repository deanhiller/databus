package models;

import java.util.ArrayList;
import java.util.List;

import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlInheritance;
import com.alvazan.orm.api.base.anno.NoSqlInheritanceType;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;

@NoSqlEntity(columnfamily="SdiTable")
@NoSqlInheritance(discriminatorColumnName="classType", strategy=NoSqlInheritanceType.SINGLE_TABLE,
   subclassesToScan={SecureTable.class, SecureSchema.class})
public abstract class SecureResource {	
	@NoSqlId
	private String id;
	
	@NoSqlOneToMany
	private List<SecureResourceGroupXref> entitiesWithAccess = new ArrayList<SecureResourceGroupXref>();

	@NoSqlManyToOne
	private EntityUser creator;
	
	public abstract String getName();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<SecureResourceGroupXref> getEntitiesWithAccess() {
		return entitiesWithAccess;
	}
	public void addUser(SecureResourceGroupXref user) {
		entitiesWithAccess.add(user);
	}

	public void setCreator(EntityUser user) {
		this.creator = user;
	}

	public EntityUser getCreator() {
		return creator;
	}
	
	public List<SecureResourceGroupXref> getAdmins() {
		List<SecureResourceGroupXref> admins = new ArrayList<SecureResourceGroupXref>();
		for(SecureResourceGroupXref ref : entitiesWithAccess) {
			if(ref.isAdminRole())
				admins.add(ref);
		}
		return admins;
	}
}
