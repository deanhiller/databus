package controllers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import models.PermissionType;
import models.SecureSchema;

public class SecurityInfo  {

	private SecureSchema theSchema;
	private Set<PermissionType> roles;

	public SecurityInfo(SecureSchema theSchema, Set<PermissionType> roles) {
		this.theSchema = theSchema;
		this.roles = roles;
	}

	public SecureSchema getTheSchema() {
		return theSchema;
	}

	public void setTheSchema(SecureSchema theSchema) {
		this.theSchema = theSchema;
	}

	public Set<PermissionType> getRoles() {
		return roles;
	}

	public void setRoles(Set<PermissionType> roles) {
		this.roles = roles;
	}

}
