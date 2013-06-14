package controllers;

import models.EntityUser;
import play.Play;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;

import controllers.auth.Secure;
import controllers.gui.auth.ActiveDirAuthentication;
import controllers.gui.auth.GuiSecure;

public class Security extends Secure.Security {

	public static boolean authenticate(String username, String password) {
		if(username == null)
			unauthorized("username cannot be null");
		else if(password == null)
			unauthorized("password cannot be null");
		
		//We have some of our own users not in ldap for development only so check entity manager first
		String prop = Play.configuration.getProperty("security.mode");
		String demomode = Play.configuration.getProperty("demo.mode");
		if("dev".equals(prop) || "true".equals(demomode)) {
			EntityUser u = findExistingUser(username);
			if(u != null && password.equals(u.getPassword())) {
				addToSession(username, u);
				return true;
			} else {
				return false;
			}
		}

		ActiveDirAuthentication auth = new ActiveDirAuthentication();
		boolean userValid = auth.isUserValid(username, password);
		if(!userValid)
			return false;
		
		EntityUser user = createUserIfNotExist(username);

		// Add them to the session
		addToSession(username, user);
		return true;
	}

	private static void addToSession(String username, EntityUser user) {
		SecurityUtil.putUser(username);
		if(user.isAdmin())
			session.put(GuiSecure.ADMIN_KEY, "true");
	}

	private static EntityUser findExistingUser(String username) {
		NoSqlEntityManager mgr = NoSql.em();
		EntityUser user = mgr.find(EntityUser.class, username);
		return user;
	}

	public static EntityUser createUserIfNotExist(String username) {
		// Second, let's check to see if the user exists in the database
		NoSqlEntityManager mgr = NoSql.em();
		EntityUser user = mgr.find(EntityUser.class, username);
		if (user != null) {
			return user;
		} else if (user == null) {
			user = EntityUser.create(username);
		} // if

		// Update the user in the sb
		mgr.put(user);
		mgr.flush();
		return user;
	}

}
