package controllers.gui.auth;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescriptorImpl {

	private static final Logger log = LoggerFactory
			.getLogger(DescriptorImpl.class);

	public static final DescriptorImpl INSTANCE = new DescriptorImpl();

	public void doDomainCheck(final String value) throws IOException,
			ServletException {
		String[] names = value.split(",");
		for (String name : names) {

			if (!name.endsWith("."))
				name += '.';

			DirContext ictx;

			// first test the sanity of the domain name itself
			try {
				if(log.isDebugEnabled())
					log.debug("Attempting to resolve " + name + " to A record");
				ictx = createDNSLookupContext();
				Attributes attributes = ictx.getAttributes(name,
						new String[] { "A" });
				Attribute a = attributes.get("A");
				if (a == null)
					throw new NamingException();
				if(log.isDebugEnabled())
					log.debug(name + " resolved to " + a.get());
			} catch (NamingException e) {
				if (log.isWarnEnabled())
	        		log.warn("Failed to resolve " + name + " to A record", e);
				throw new IllegalArgumentException("Invalid domain name="
						+ value + " check chained exception", e);
			}

			// then look for the LDAP server
			final String ldapServer = "_ldap._tcp." + name;
			String serverHostName;
			try {
				serverHostName = obtainLDAPServer(ictx, name);
			} catch (NamingException e) {
				if (log.isWarnEnabled())
	        		log.warn("Failed to resolve " + ldapServer + " to SRV record",
						e);
				throw new IllegalStateException(
						"No SRV record for ldap server found in "
								+ name
								+ " domain.  On linux run this command to see 'dig SRV _ldap._tcp."
								+ name + "'");
			}

			// try to connect to LDAP port to make sure this machine has LDAP
			// service
			// TODO: honor the port number in SRV record
			try {
				new Socket(serverHostName, 389).close();
			} catch (IOException e) {
				if (log.isWarnEnabled())
	        		log.warn("Failed to connect to LDAP port", e);
				throw new IllegalArgumentException(
						"your ldap server does not have port 389 open="
								+ serverHostName);
			}
		}
		// looks good
	}

	/**
	 * Creates {@link DirContext} for accesssing DNS.
	 */
	public DirContext createDNSLookupContext() throws NamingException {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");
		return new InitialDirContext(env);
	}

	public String obtainLDAPServer(String domainName) throws NamingException {
		return obtainLDAPServer(createDNSLookupContext(), domainName);
	}

	/**
	 * Use DNS and obtains the LDAP server's host name.
	 */
	public String obtainLDAPServer(DirContext ictx, String domainName)
			throws NamingException {
		final String ldapServer = "_ldap._tcp." + domainName;

		if(log.isDebugEnabled())
			log.debug("Attempting to resolve " + ldapServer + " to SRV record");
		Attributes attributes = ictx.getAttributes(ldapServer,
				new String[] { "SRV" });
		Attribute a = attributes.get("SRV");
		if (a == null)
			throw new NamingException();

		int priority = -1;
		String result = null;
		for (NamingEnumeration ne = a.getAll(); ne.hasMoreElements();) {
			String[] fields = ne.next().toString().split(" ");
			int p = Integer.parseInt(fields[0]);
			if (priority == -1 || p < priority) {
				priority = p;
				result = fields[3];
				// cut off trailing ".". HUDSON-2647
				result = result.replace("\\.$", "");
			}
		}
		if(log.isDebugEnabled())
			log.debug(ldapServer + " resolved to " + result);
		return result;
	}
}
