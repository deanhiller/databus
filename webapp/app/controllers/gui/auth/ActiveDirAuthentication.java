package controllers.gui.auth;
import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

import com.sun.jndi.ldap.LdapCtxFactory;


public class ActiveDirAuthentication {

	private static final Logger log = LoggerFactory.getLogger(ActiveDirAuthentication.class);
	
	public boolean isUserValid(String username, String password, String domainName) {
		try {
			String demomode = Play.configuration.getProperty("demo.mode");
			if("true".equals(demomode))
				return false;
			
			retrieveUser(username, password, domainName);
			return true;
		} catch(InvalidCredentialsException e) {
			if (log.isInfoEnabled())
				log.info("Failed to login="+username+" domain="+domainName, e);
			return false;
		}
	}
    public void retrieveUser(String username, String password, String domainName)  {
        // bind by using the specified username/password
        String principalName = username + '@' + domainName;

        DirContext context = fetchContext(password, domainName, principalName);

        try {
        	fetchInfo(username, password, domainName, principalName, context);
        } catch(NamingException e) {
        	if (log.isWarnEnabled())
        		log.warn("Failed to retrieve user information for "+username,e);
            throw new InvalidCredentialsException("Failed to retrieve user information for "+username,e);
        }
    }

	private void fetchInfo(String username, String password, String domainName,
			String principalName, DirContext context) throws NamingException {
		// locate this user's record
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> renum = context.search(toDC(domainName),"(& (userPrincipalName="+principalName+")(objectClass=user))", controls);
        if(!renum.hasMore()) {
            // failed to find it. Fall back to sAMAccountName.
            // see http://www.nabble.com/Re%3A-Hudson-AD-plug-in-td21428668.html
            renum = context.search(toDC(domainName),"(& (sAMAccountName="+username+")(objectClass=user))", controls);
            if(!renum.hasMore())
                throw new InvalidCredentialsException("Authentication was successful but cannot locate the user information for "+username);
        }
        SearchResult result = renum.next();


        List<GrantedAuthority> groups = new ArrayList<GrantedAuthority>();
        Attribute memberOf = result.getAttributes().get("memberOf");
        if(memberOf!=null) {// null if this user belongs to no group at all
            for(int i=0; i<memberOf.size(); i++) {
                Attributes atts = context.getAttributes("\"" + memberOf.get(i) + '"', new String[]{"CN"});
                Attribute att = atts.get("CN");
                groups.add(new GrantedAuthorityImpl(att.get().toString()));
            }
        }

        context.close();

        if (log.isInfoEnabled())	
	        for(GrantedAuthority g : groups) {
	        	log.info("group ="+g);
	        }
      
	}

	public static DirContext fetchCtx(String domain, String username, String password) {
		String principalName = username + '@' + domain;
		return fetchContext(password, domain, principalName);
	}
	
	private static DirContext fetchContext(String password, String domainName, String principalName) {
		Hashtable props = new Hashtable();
        props.put(Context.SECURITY_PRINCIPAL, principalName);
        props.put(Context.SECURITY_CREDENTIALS,password);
        props.put(Context.REFERRAL, "follow");
        DirContext context;

        try {
            String inst = DescriptorImpl.INSTANCE.obtainLDAPServer(domainName) ;
            context = LdapCtxFactory.getLdapCtxInstance(
                    "ldap://" + inst+ '/',
                    props);
        } catch (NamingException e) {
        	if (log.isInfoEnabled())
    			log.info("Failed to bind to LDAP",e);
            throw new InvalidCredentialsException("Either no such user '"+principalName+"' or incorrect password",e);
        }
		return context;
	}

    private static String toDC(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0)   continue;   // defensive check
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }


}
