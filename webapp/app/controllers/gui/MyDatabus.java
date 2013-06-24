package controllers.gui;

import gov.nrel.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.EntityUser;
import models.SecureResourceGroupXref;
import models.SecureSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;

import com.alvazan.play.NoSql;

import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MyDatabus extends Controller {
	static final Logger log = LoggerFactory.getLogger(MyDatabus.class);
	
	public static void myDatabus() {
		EntityUser user = Utility.getCurrentUser(session);

		List<EntityGroup> allGroups = EntityGroup.findAll(NoSql.em());
		List<SecureSchema> allDatabases = SecureSchema.findAll(NoSql.em());
		
		List<EntityGroup> systemGroups = new ArrayList<EntityGroup>();
		List<EntityGroup> myGroups = new ArrayList<EntityGroup>();
		
		List<SecureSchema> systemDatabases = new ArrayList<SecureSchema>();
		Map<SecureSchema, String> readableDatabases = new HashMap<SecureSchema, String>();
		Map<SecureSchema, String> readwriteDatabases = new HashMap<SecureSchema, String>();
		Map<SecureSchema, String> myDatabases = new HashMap<SecureSchema, String>();

		for (SecureSchema db : allDatabases) {
			List<SecureResourceGroupXref> refs = db.getEntitiesWithAccess();
			
			boolean foundUser = false;
			
			for (SecureResourceGroupXref ref : refs) {
				Entity entity = ref.getUserOrGroup();
								
				if(entity.getName() == null) {
					continue;
				}
				
				if(entity.getClassType().toLowerCase().equals("userimpl")) {
					if(entity.getName().equals(user.getName())) {
						foundUser = true;
						/**
						 * Ok we found the current user in the list here... lets remember their permission
						 */
						switch(ref.getPermission()) {
							case ADMIN: {
								myDatabases.put(db, user.getName());
								break;
							}
							case READ: {
								readableDatabases.put(db, user.getName());
								break;
							}
							case READ_WRITE: {
								readwriteDatabases.put(db, user.getName());
								break;
							}
							default: {
								systemDatabases.add(db);
								break;
							}
						}
						
						/**
						 * We'll continue because its possible that the user might also be in a group
						 * associated w/ this database
						 */
					}
				} else if(entity.getClassType().toLowerCase().equals("group")) {
					EntityGroup group = (EntityGroup)entity;
					List<EntityGroupXref> users = group.getChildren();
					
					for(EntityGroupXref member : users) {
						if(member.getEntity() == null) {
							continue;
						}
						
						if(member.getEntity().getName() == null) {
							continue;
						}
						
						if(member.getEntity().getName().equals(user.getName())) {
							foundUser = true;
							
							switch(ref.getPermission()) {
								case ADMIN: {
									myDatabases.put(db, group.getName() + " (Group)");
									break;
								}
								case READ: {
									readableDatabases.put(db, group.getName() + " (Group)");
									break;
								}
								case READ_WRITE: {
									readwriteDatabases.put(db, group.getName() + " (Group)");
									break;
								}
								default: {
									systemDatabases.add(db);
									break;
								}
							}
						}						
					}
				}
			}
			
			if(!foundUser) {
				systemDatabases.add(db);
			}
		}
		
		for(EntityGroup group : allGroups) {
			List<EntityGroupXref> childGroups = group.getChildren();
			
			boolean admin = false;
			for(EntityGroupXref childGroup : childGroups) {
				
				if(childGroup.getEntity().getName().equals(user.getName())) {
					admin = true;
					break;
				}
			}
			
			if(admin) {
				myGroups.add(group);
			} else {
				systemGroups.add(group);
			}
		}
		
		String sid = session.get("sid");

		/*
		if (log.isInfoEnabled())
			log.info("MyDatabus.myDatabus() INFO: render index. user=" + user + " num tables=" + allDatabases.size()
				+ " groupscnt=" + allGroups.size() + " session=" + sid);
		*/
		render(myGroups, systemGroups, systemDatabases, myDatabases, readableDatabases, readwriteDatabases);
	}

}
