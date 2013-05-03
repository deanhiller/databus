package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;

@NoSqlDiscriminatorColumn("group")
@NoSqlQueries({
	@NoSqlQuery(name="findAll", query="select e from TABLE as e"),
	@NoSqlQuery(name="findByName", query="select e from TABLE as e where e.name = :group")
})
public class EntityGroup extends Entity {

	//NOTE: WE really need an index on this subclass so we can query just the subclass so this is 
	//really just for that...
	@NoSqlIndexed
	private String name;
	
	private String description;
	
	@NoSqlOneToMany
	private List<EntityGroupXref> children = new ArrayList<EntityGroupXref>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<EntityGroupXref> getChildren() {
		return children;
	}

	public static List<EntityGroup> findAll(NoSqlEntityManager em) {
		Query<EntityGroup> query = em.createNamedQuery(EntityGroup.class, "findAll");
		Cursor<KeyValue<EntityGroup>> cursor = query.getResults("name");
		List<EntityGroup> groups = new ArrayList<EntityGroup>();
		while(cursor.next()) {
			KeyValue<EntityGroup> kv = cursor.getCurrent();
			if(kv.getValue() == null)
				continue;
			groups.add(kv.getValue());
		}
		return groups;
	}

	public static EntityGroup findByName(NoSqlEntityManager em, String group) {
		Query<EntityGroup> query = em.createNamedQuery(EntityGroup.class, "findByName");
		query.setParameter("group", group);
		return query.getSingleObject();
	}

	protected void addChild(EntityGroupXref entityGroupXref) {
		this.children.add(entityGroupXref);
	}

	public void getRobots(Set<String> processedIds, List<EntityUser> robots) {
		if(processedIds.contains(getId()))
			return;
		processedIds.add(getId());
		
		for(EntityGroupXref child : getChildren()) {
			Entity entity = child.getEntity();
			if(!(entity instanceof EntityUser))
				continue;
			EntityUser user = (EntityUser) entity;
			if(user.getType() == UserType.ROBOT)
				robots.add(user);
		}
		return;
	}

	@Override
	public String getTypeString() {
		return "Group";
	}

	@Override
	public UserType getType() {
		return UserType.GROUP;
	}
}
