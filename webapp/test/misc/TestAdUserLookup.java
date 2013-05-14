package misc;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.junit.Test;

import controllers.gui.auth.ActiveDirAuthentication;
import controllers.gui.auth.ActiveDirectory;

public class TestAdUserLookup {

	public void lookupUser() throws NamingException {
		ActiveDirectory dir = new ActiveDirectory("dhiller2", "putPasswordHereToWorkRight", "nrel.gov");
		
		NamingEnumeration<SearchResult> results = dir.searchUser("jcollins", "username", null);
		while(results.hasMore()) {
			SearchResult res = results.next();
			Attributes attrs = res.getAttributes();
			String temp = attrs.get("samaccountname").toString();
			System.out.println("Username	: " + temp.substring(temp.indexOf(":")+1));
			temp = attrs.get("givenname").toString();
			System.out.println("Name         : " + temp.substring(temp.indexOf(":")+1));
			temp = attrs.get("mail").toString();
			System.out.println("Email ID	: " + temp.substring(temp.indexOf(":")+1));
			temp = attrs.get("cn").toString();
			System.out.println("Display Name : " + temp.substring(temp.indexOf(":")+1) + "\n\n"); 
		}
		
		dir.closeLdapConnection();
	}
}
