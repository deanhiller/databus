package controllers.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.KeyToTableName;
import models.PermissionType;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import play.data.validation.Validation;
import play.mvc.Scope.Flash;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.play.NoSql;

public class MySchemaLogic {
	static final Logger log = LoggerFactory.getLogger(MySchemaLogic.class);
	
	public static void createXref(SecureSchema schema, String permission,
			Entity entity) {
		List<SecureResourceGroupXref> users = schema.getEntitiesWithAccess();
		for(SecureResourceGroupXref u : users) {
			if(u.getUserOrGroup().getId().equals(entity.getId())) {
				Validation.current().addError("user.name", "This user is already in this schema");
				break;
			}
		}
	
		if (Validation.current().hasErrors()) {
			Flash.current().error("This user is already in this schema");
			MyDatabases.dbProperties(schema.getSchemaName());
		}
		
		PermissionType perm = PermissionType.lookup(permission);
		SecureResourceGroupXref mapping = new SecureResourceGroupXref(entity, schema, perm);
	
		NoSql.em().fillInWithKey(mapping);
		if(log.isInfoEnabled())
			log.info("adding entity="+entity.getName()+" to resource="+schema.getName()+" mapping id="+mapping.getId());
		NoSql.em().put(schema);
		NoSql.em().put(entity);
		NoSql.em().put(mapping);
		
		NoSql.em().flush();
		
		//done after flush as this could be a huge put/put/put so need to flush every 50 entries...
		Counter c = new Counter();
		CursorToMany<SecureTable> cursor = schema.getTablesCursor();
		Set<String> processedIds = new HashSet<String>();
		fillInKeyToTableRows(cursor, entity, perm, c, processedIds);
	
		NoSql.em().flush();
	
		if (log.isInfoEnabled())
			log.info("updated keyToTable rows of count="+c.getTotalCount()+" for addition of entity="+entity.getName());
		
	}
	
	private static void fillInKeyToTableRows(CursorToMany<SecureTable> cursor, Entity entity, PermissionType perm, Counter count, Set<String> processedIds) {
		if(processedIds.contains(entity.getId()))
			return; //if we already processed it, then return.
		processedIds.add(entity.getId()); //now add the fact that we processed it before we recurse
		
		if(entity instanceof EntityUser) {
			createRows(cursor, (EntityUser) entity, perm, count);
		} else if(entity instanceof EntityGroup) {
			EntityGroup group = (EntityGroup) entity;
			List<EntityGroupXref> children = group.getChildren();
			for(EntityGroupXref ref : children) {
				fillInKeyToTableRows(cursor, ref.getEntity(), perm, count, processedIds);
			}
		} else
			throw new RuntimeException("bug, entity type not supported="+entity.getClass().getName());
	}

	public static void addKeyToTableNames(SecureSchema db, EntityUser user, SecureResourceGroupXref xref) {
		Counter c = new Counter();
		createRows(db.getTablesCursor(), user, xref.getPermission(), c);
		
		if (log.isInfoEnabled())
			log.info("count of keytotablenames="+c.getTotalCount()+" for addition of entity="+user.getName());
	}

	private static void createRows(CursorToMany<SecureTable> cursor, EntityUser entity, PermissionType perm, Counter count) {
		//reset the cursor every time before we use it....
		cursor.beforeFirst();
		
		while(cursor.next()) {
			SecureTable table = cursor.getCurrent();
			String key = KeyToTableName.formKey(table.getName(), entity.getUsername(), entity.getApiKey());
			KeyToTableName kt = new KeyToTableName(key, table, perm);
			NoSql.em().put(kt);
			count.increment();
			if(count.getCount() >= 50) {
				count.reset();
				NoSql.em().flush();
				NoSql.em().clear(); //so we don't run out of memory, we need to clear the 1st level cache
			}
		}
	}

	static class Counter {
		private int count = 0;
		private int total = 0;
		public void increment() {
			count++;
			total++;
		}
		public int getTotalCount() {
			return total;
		}
		public void reset() {
			count = 0;
		}
		public int getCount() {
			return count;
		}
	}

}
