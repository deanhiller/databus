package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.alvazan.orm.api.base.ToOneProvider;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;

@NoSqlEntity
public class EntityGroupXref {

	@NoSqlId
	private String id;
	
	@NoSqlManyToOne
	private ToOneProvider<Entity> user = new ToOneProvider<Entity>();
	
	@NoSqlManyToOne
	private EntityGroup group;

	private boolean isGroupAdmin;

	/**
	 * Between EntityRobot and EntityGroup, there is always ONE owning group such that he is the only one who can delete 
	 * the actual robot if he desires or in the future potentially regenerate his keys if desired as well.
	 */
	private boolean isOwnRobot;
	
	public EntityGroupXref(EntityGroup group, Entity entity, boolean isGroupAdmin, boolean isOwnRobot) {
		this.group = group;
		this.user.set(entity);
		group.addChild(this);
		entity.addParentGroup(this);
		this.isGroupAdmin = isGroupAdmin;
		this.isOwnRobot = isOwnRobot;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Entity getEntity() {
		return user.get();
	}

	protected void setEntity(Entity user) {
		this.user.set(user);
	}

	public EntityGroup getGroup() {
		return group;
	}

	protected void setGroup(EntityGroup group) {
		this.group = group;
	}

	public boolean isGroupAdmin() {
		return isGroupAdmin;
	}

	public void setGroupAdmin(boolean isAdmin) {
		this.isGroupAdmin = isAdmin;
	}

	public boolean isOwnRobot() {
		return isOwnRobot;
	}

	public void setOwnRobot(boolean isOwnRobot) {
		this.isOwnRobot = isOwnRobot;
	}

	public String getPermission() {
		if(isGroupAdmin)
			return "Group Admin";
		return "";
	}

	public void getRobots(Set<String> processedIds, List<EntityUser> robots) {
		if(isGroupAdmin)
			getGroup().getRobots(processedIds, robots);
	}
	
}
