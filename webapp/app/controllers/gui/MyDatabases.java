package controllers.gui;

import gov.nrel.util.ATriggerListener;
import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.DataTypeEnum;
import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.KeyToTableName;
import models.PermissionType;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.message.Trigger;

import org.apache.commons.lang.StringUtils;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.play.NoSql;

import controllers.TableMonitor;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MyDatabases extends Controller {
	
	static final Logger log = LoggerFactory.getLogger(MyDatabases.class);
	
	public static void myDatabases() {
		EntityUser user = Utility.getCurrentUser(session);

		List<SecureResourceGroupXref> resouceXrefs = user.getResources();
		if (log.isInfoEnabled())
			for (SecureResourceGroupXref resource : resouceXrefs) {
				log.info("resource=" + resource.getResource());
			}
		
		List<SecureSchema> databases = new ArrayList<SecureSchema>();
		for(SecureResourceGroupXref resource: user.getSchemas()) {
			String dbName = resource.getResource().getName();
			
			SecureSchema database = SecureSchema.findByName(NoSql.em(), dbName);
			databases.add(database);
			
			log.info("Added DB: [" + database.getName() + "]");
		}

		render(user, databases);
	}

	public static void viewDatabaseImpl(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.READ) || roles.contains(PermissionType.READ_WRITE)
					|| roles.contains(PermissionType.ADMIN)) {
				tables = schema.getTables(0, 50);
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else
				viewDatabase(schemaName);
		}

		List<String> ids = schema.getMonitorIds();
		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> mons = svc.getMonitors(ids);
		List<TableMonitor> monitors = new ArrayList<TableMonitor>();
		for(PlayOrmCronJob m : mons) {
			monitors.add(TableMonitor.copy(m));
		}
		DataTypeEnum timeseries = DataTypeEnum.TIME_SERIES;
		render(user, schema, tables, monitors, timeseries);
	}
	
	public static void viewDatabase(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.ADMIN)) {
				dbTables(schemaName);
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else {
				tables = schema.getTables(0, 50);
			}
		}
		
		viewDatabaseImpl(schemaName);
	}
	
	public static void postDbUserDelete(String schemaName, String xrefId) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		
		SecureResourceGroupXref ref = NoSql.em().find(SecureResourceGroupXref.class, xrefId);
		String id = ref.getResource().getId();
		if(!id.equals(schema.getId()))
			notFound("this schema and xref are not tied together");

		//First, let's remove all the KeyToTableNames that the user may be in directly
		Entity entity = ref.getUserOrGroup();
		removeKeyToTableRows(schema, entity);
		
		schema.getEntitiesWithAccess().remove(ref);
		entity.getResources().remove(ref);
		
		NoSql.em().put(schema);
		NoSql.em().put(entity);
		NoSql.em().remove(ref);

		NoSql.em().flush();

		dbUsers(schemaName);
	}

	private static void removeKeyToTableRows(SecureSchema schema, Entity entity) {
		Counter c = new Counter();
		Set<EntityUser> users = Utility.findAllUsers(entity);
		Utility.removeKeyToTableRows(schema, users, c);
	}

	public static void dbUsers(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		schema.setCreator(user);
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.ADMIN)) {
				tables = schema.getTables(0, 50);				
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else
				viewDatabase(schemaName);
		}
		
		String oldSchemaName = schema.getSchemaName();
		List<String> ids = schema.getMonitorIds();
		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> mons = svc.getMonitors(ids);
		List<TableMonitor> monitors = new ArrayList<TableMonitor>();
		for(PlayOrmCronJob m : mons) {
			if(m != null)
				monitors.add(TableMonitor.copy(m));
		}
		DataTypeEnum timeseries = DataTypeEnum.TIME_SERIES;
		render(user, schema, oldSchemaName, tables, monitors, timeseries);
	}
	
	public static void dbCronJobs(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		schema.setCreator(user);
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.ADMIN)) {
				tables = schema.getTables(0, 50);				
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else
				viewDatabase(schemaName);
		}
		
		String oldSchemaName = schema.getSchemaName();
		List<String> ids = schema.getMonitorIds();
		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> mons = svc.getMonitors(ids);
		List<TableMonitor> monitors = new ArrayList<TableMonitor>();
		for(PlayOrmCronJob m : mons) {
			if(m != null)
				monitors.add(TableMonitor.copy(m));
		}
		
		List<String> triggerIds = schema.getTriggerIds();
		List<PlayOrmCronJob> trigs = svc.getMonitors(triggerIds);
		List<Trigger> triggers = new ArrayList<Trigger>();
		for(PlayOrmCronJob m : trigs) {
			if(m != null) {
				Trigger trigger = ATriggerListener.transform(m);
				triggers.add(trigger);
			}
		}
		
		render(user, schema, monitors, triggers);
	}
	
	public static void dbTables(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		schema.setCreator(user);
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.ADMIN)) {
				tables = schema.getTables(0, 50);				
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else
				viewDatabase(schemaName);
		}
		
		String oldSchemaName = schema.getSchemaName();
		List<String> ids = schema.getMonitorIds();
		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> mons = svc.getMonitors(ids);
		List<TableMonitor> monitors = new ArrayList<TableMonitor>();
		for(PlayOrmCronJob m : mons) {
			if(m != null)
				monitors.add(TableMonitor.copy(m));
		}
		DataTypeEnum timeseries = DataTypeEnum.TIME_SERIES;
		render(user, schema, oldSchemaName, tables, monitors, timeseries);
	}

	public static void dbProperties(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema schema = new SecureSchema();
		schema.setCreator(user);
		List<SecureTable> tables = new ArrayList<SecureTable>();
		if (schemaName != null) {
			schema = SecureSchema.findByName(NoSql.em(), schemaName);
			Set<PermissionType> roles = fetchRoles(schema, user);
			if(roles.contains(PermissionType.ADMIN)) {
				tables = schema.getTables(0, 50);				
			} else if(roles.size() == 0) {
				notFound("Your user does not have access to this resource");
			} else
				viewDatabase(schemaName);
		}
		
		String oldSchemaName = schema.getSchemaName();
		List<String> ids = schema.getMonitorIds();
		CronService svc = CronServiceFactory.getSingleton(null);
		List<PlayOrmCronJob> mons = svc.getMonitors(ids);
		List<TableMonitor> monitors = new ArrayList<TableMonitor>();
		for(PlayOrmCronJob m : mons) {
			if(m != null)
				monitors.add(TableMonitor.copy(m));
		}
		DataTypeEnum timeseries = DataTypeEnum.TIME_SERIES;
		render(user, schema, oldSchemaName, tables, monitors, timeseries);
	}

	private static SecureSchema schemaCheck(String schema, EntityUser user, PermissionType permission) {
		SecureSchema theSchema = SecureSchema.findByName(NoSql.em(), schema);
		Set<PermissionType> roles = fetchRoles(theSchema, user);
		if(!roles.contains(permission))
			notFound("Your user does not have access to this resource");

		return theSchema;
	}

	private static Set<PermissionType> fetchRoles(SecureResource resource, EntityUser user) {
		if(resource == null)
			notFound("You user either does not have access or this schema does not exist");
		
		if (log.isInfoEnabled())
			log.info("resource found="+resource.getId()+" n2="+resource.getName());
		if(user.isAdmin()) {
			return PermissionType.allRoles();
		}

		Set<PermissionType> roles = new HashSet<PermissionType>();
		user.addResources(roles, resource);
		
		if(resource instanceof SecureTable) {
			if(log.isDebugEnabled())
				log.debug("resource is a table so adding roles for table");
			SecureTable t = (SecureTable) resource;
			user.addResources(roles, t.getSchema());
		}
		else {
			if(log.isDebugEnabled())
				log.debug("resource "+resource.getName()+" is NOT a table so NOT adding roles for table.  Resource.getClass() is "+resource.getClass());
		}

		if(roles.size() == 0)
			notFound("Your user either does not have access or this schema does not exist");
		
		return roles;
	}
	
	private static void postNewDatabase(SecureSchema schema) {
		schema.setId(null); //playframework set this to "" for some reason(need to fix playframework there for Strings
		
		if (log.isInfoEnabled())
			log.info(" schema id=" + schema.getId() + " name=" + schema.getSchemaName());
		EntityUser user = Utility.getCurrentUser(session);

		// This is a new schema or name change, ensure name does not already
		// exist
		SecureSchema existing = SecureSchema.findByName(NoSql.em(),
				schema.getSchemaName());
		if (existing != null) {
			// If there is already a schema and it is not this schema(ie. ids are
			// different) we then have a problem...
			validation.addError("schema.schemaName",
					"This schema name is in use already");
		}

		if (validation.hasErrors()) {
			render("@editSchema", user, schema);
		}

		NoSql.em().fillInWithKey(schema);
		SecureResourceGroupXref xref = new SecureResourceGroupXref(user, schema, PermissionType.ADMIN);		
		NoSql.em().put(xref);
		
		NoSql.em().put(schema);
		NoSql.em().put(user);

		NoSql.em().flush();

		myDatabases();
	}
	
	private static void postModifyDatabase(SecureSchema schema, String oldSchemaName) {

		if (log.isInfoEnabled())
			log.info(" schema id=" + schema.getId() + " name=" + schema.getSchemaName());
		EntityUser user = Utility.getCurrentUser(session);

		SecureSchema oldSchema = schemaCheck(oldSchemaName, user, PermissionType.ADMIN);

		if(!schema.getSchemaName().equals(oldSchema.getSchemaName())) {
			// This is a name change, ensure name does not already exist BUT can exist if id is same(ie. same schema)
			SecureSchema existing = SecureSchema.findByName(NoSql.em(), schema.getSchemaName());
			if (existing != null && !existing.getId().equals(schema.getId())) {
				// If there is already a group and it is not this group(ie. ids are
				// different) we then have a problem...
				validation.addError("schema.name",
						"This schema name is in use already");
			}
		}

		if (validation.hasErrors()) {
			render("@editSchema", user, schema);
		}

		NoSql.em().put(schema);
		NoSql.em().flush();
	}
	
	public static void tableAdd(String schema) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema actualSchema = schemaCheck(schema, user, PermissionType.ADMIN);
		
		SecureResource table = new SecureTable();
		render(actualSchema, table);
	}
	
	
	public static void postDatabase(SecureSchema schema, String oldSchemaName) {
		if(StringUtils.isEmpty(oldSchemaName)) {
			postNewDatabase(schema);
		} else 
			postModifyDatabase(schema, oldSchemaName);

		//TODO:JSC reindex search data for altered/new schema here!
		myDatabases();
	}
	
	public static void editDatabase(String edit_dbName, String edit_dbDescription) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schemaDbo = schemaCheck(edit_dbName, user, PermissionType.ADMIN);
		
		if(schemaDbo == null) {
			/**
			 * How to do the field.error code?
			 */
			validation.addError("database", "Database [" + edit_dbName + "] does not exist.");
			//myDatabases();
		}
		
		schemaDbo.setDescription(edit_dbDescription);

		if (log.isInfoEnabled())
			log.info(" Editing Database id=" + schemaDbo.getId() + " name=" + schemaDbo.getSchemaName());

		if (validation.hasErrors()) {
			render("@editSchema", user, schemaDbo);
		}

		NoSql.em().put(schemaDbo);
		NoSql.em().flush();
		
		myDatabases();
	}

	public static void addEditDatabase(SecureSchema postedDatabase) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema databaseToSave = null;
		String successMsg = "";
		
		if((postedDatabase.getSchemaName() == null) || (postedDatabase.getSchemaName().equals(""))) {
			// We cannot have an empty db name
			validation.addError("postedDatabase.schemaName", "The database name cannot be empty!");
		} else {
			databaseToSave = SecureSchema.findByName(NoSql.em(), postedDatabase.getSchemaName());
			if (databaseToSave == null) {
				/**
				 * This is a new db
				 */
				if (log.isInfoEnabled())
					log.info(" Adding new database: name=" + postedDatabase.getSchemaName());
				
				databaseToSave = new SecureSchema();
				databaseToSave.setId(null);
				databaseToSave.setSchemaName(postedDatabase.getSchemaName());
				databaseToSave.setDescription(postedDatabase.getDescription());
				
				NoSql.em().fillInWithKey(databaseToSave);
				SecureResourceGroupXref xref = new SecureResourceGroupXref(user, databaseToSave, PermissionType.ADMIN);		
				NoSql.em().put(xref);
				
				NoSql.em().put(databaseToSave);
				NoSql.em().put(user);
				NoSql.em().flush();
				
				successMsg = "Database \"" + postedDatabase.getSchemaName() + "\" has been added.";
			} else {
				/**
				 * This is an edit to an existing db
				 */
				Set<PermissionType> roles = fetchRoles(postedDatabase, user);
				if(!roles.contains(PermissionType.ADMIN)) {
					/**
					 * Lack of permissions requires an error that does not bring up the edit modal.  We'll
					 * just use the flash here and redirect to the myDatabases() controller call.
					 */
					flash.error("You do not have permissions to edit the \"" + postedDatabase.getSchemaName() + "\" database.");
					flash.put("showModal", "true");
					flash.put("isError", "true");
					myDatabases();
					return;
				}
				
				/**
				 * This edit of a database can ONLY change the description.  So, lets update the databaseToSave object
				 * with the postedDatabase description since we do NOT save all fields in the html for any given database
				 */
				databaseToSave.setDescription(postedDatabase.getDescription());
				
				NoSql.em().put(databaseToSave);
				NoSql.em().flush();
				
				successMsg = "Database \"" + postedDatabase.getSchemaName() + "\" has been updated.";
			}
		}

		if (validation.hasErrors()) {
			boolean isError = true;
			boolean showModal = true;

			// Get the databases for the render
			List<SecureSchema> databases = new ArrayList<SecureSchema>();
			for(SecureResourceGroupXref resource: user.getSchemas()) {
				String dbName = resource.getResource().getName();
				databases.add(SecureSchema.findByName(NoSql.em(), dbName));
			}
			
			render("@myDatabases", databases, postedDatabase, isError, showModal);
			return;
		}
		
		flash.success(successMsg);
		
		myDatabases();
	}

	public static void monitorAdd(String schemaName) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		render("@monitorEdit", schema);
	}
	
	public static void monitorEdit(String schemaName, String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		
		String id = TableMonitor.formKey(schemaName, table);
		CronService svc = CronServiceFactory.getSingleton(null);
		PlayOrmCronJob mon = svc.getMonitor(id);
		if(mon == null)
			notFound();
		
		TableMonitor monitor = TableMonitor.copy(mon);
		
		render(schema, monitor);
	}
	
	public static void postDbTriggerDelete(String schemaName, String cronId) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		
		String id = ATriggerListener.formId(cronId);
		schema.getTriggerIds().remove(id);
		CronService svc = CronServiceFactory.getSingleton(null);
		svc.deleteMonitor(id);

		NoSql.em().put(schema);
		NoSql.em().flush();

		dbCronJobs(schemaName);
	}

	public static void postMonitor(String schemaName, TableMonitor monitor) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		
		SecureTable table = SecureTable.findByName(NoSql.em(), monitor.getTableName());
		if(table == null) {
			validation.addError("monitor.tableName", "This table does not exist");
		} else if(!table.getSchema().getName().equals(schemaName)) {
			validation.addError("monitor.tableName", "This table does not exist in this schema");
		}
		
		if(validation.hasErrors()) {
			render("@monitorEdit", schema, monitor);
			return;
		}
		
		PlayOrmCronJob playMonitor = TableMonitor.copy(schemaName, monitor);
		CronService svc = CronServiceFactory.getSingleton(null);
		svc.saveMonitor(playMonitor);
		
		List<String> ids = schema.getMonitorIds();
		ids.add(playMonitor.getId());
		
		NoSql.em().put(schema);
		NoSql.em().flush();

		dbCronJobs(schemaName);
	}

	public static void dbUserAdd(String schemaName, String type) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);
		render("@dbUserEdit", schema, type);
	}
	
	public static void dbUserEdit(String schemaName, String type, String id) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);

		SecureResourceGroupXref xref = null;
		List<SecureResourceGroupXref> xrefs = schema.getEntitiesWithAccess();
		for(SecureResourceGroupXref ref : xrefs) {
			Entity entity = ref.getUserOrGroup();
			if(entity.getName().equals(id)) {
				if("group".equals(type) && entity instanceof EntityGroup) {
					xref = ref;
					break;
				} else if("user".equals(type) && entity instanceof EntityUser) {
					xref = ref;
					break;
				}
			}
		}

		if(xref == null)
			notFound("sorry, you might have a bad url or it could be a bug too");
		
		String permission = xref.getPermission().value();
		render(schema, type, xref, permission);
	}
	
	public static void postUserDb(String schemaName, String type, String name, String xrefId, String permission) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema schema = schemaCheck(schemaName, user, PermissionType.ADMIN);

		PermissionType p = PermissionType.lookup(permission);
		SecureResourceGroupXref xref;
		if(xrefId == null || "".equals(xrefId)) {
			Entity entity;
			if("group".equals(type)) {
				entity = EntityGroup.findByName(NoSql.em(), name);
				if (entity == null) {
					flash.error("The group="+name+" does not exist");
					flash.keep();
					MyDatabases.dbUserAdd(schemaName, type);
				}
			} else {
				entity = EntityUser.findByName(NoSql.em(), name);
				if (entity == null) {
					flash.error("The user="+name+" does not exist.  Please ask them to login to the system once first!!!");
					flash.keep();
					MyDatabases.dbUserAdd(schema.getSchemaName(), type);
				}
			}

			xref = new SecureResourceGroupXref(entity, schema, p);
			xref.setPermission(p);
			NoSql.em().put(xref);
			NoSql.em().put(schema);
			NoSql.em().put(entity);
		} else {
			
			for(SecureResourceGroupXref ref : schema.getEntitiesWithAccess()) {
				Entity ent = ref.getUserOrGroup();
				if(ent.getName().equals(name)) {
					if(ent instanceof EntityGroup && "group".equals(type)) {
						flash.error("The group="+name+" already has access to this database");
						flash.keep();
						MyDatabases.dbUserAdd(schemaName, type);
					} else if(ent instanceof EntityUser && "user".equals(type)) {
						flash.error("The user="+name+" already has access to this database");
						flash.keep();
						MyDatabases.dbUserAdd(schemaName, type);
					}
				}
			}
			
			xref = NoSql.em().find(SecureResourceGroupXref.class, xrefId);
			xref.setPermission(p);
			NoSql.em().put(xref);
		}

		NoSql.em().flush();

		dbUsers(schemaName);
	}

}
