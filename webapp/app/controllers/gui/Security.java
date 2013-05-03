package controllers.gui;

import models.EntityUser;
import play.Play;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;

import controllers.auth.ActiveDirAuthentication;
import controllers.auth.Secure;
import controllers.gui.auth.GuiSecure;

public class Security extends GuiSecure.Security {

	static boolean authenticate(String username, String password) {
		if(username == null)
			unauthorized("username cannot be null");
		else if(password == null)
			unauthorized("password cannot be null");
		
		//We have some of our own users not in ldap for development only so check entity manager first
		String prop = Play.configuration.getProperty("security.mode");
		if("dev".equals(prop)) {
			EntityUser u = findExistingUser(username);
			if(u != null) {
				addToSession(username, u);
				return true;
			}
		}

		ActiveDirAuthentication auth = new ActiveDirAuthentication();
		boolean userValid = auth.isUserValid(username, password, "nrel.gov");
		if(!userValid)
			return false;
		
		EntityUser user = createUserIfNotExist(username);

		// Add them to the session
		addToSession(username, user);
		return true;
	}

	private static void addToSession(String username, EntityUser user) {
		session.put("username", username);
		if(user.isAdmin())
			session.put("admin", "true");
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
