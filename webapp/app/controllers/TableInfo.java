package controllers;

import java.util.ArrayList;
import java.util.List;

import models.RoleMapping;
import models.SecureResource;
import models.SecurityGroup;

public class TableInfo {

	private SecureResource table;
	private RoleMapping mapping;
	private List<SecurityGroup> groups = new ArrayList<SecurityGroup>();
	private List<String> groupNames = new ArrayList<String>();
	public TableInfo(SecureResource t, SecurityGroup group, RoleMapping r) {
		this.table = t;
		this.mapping = r;
		this.groups.add(group);
		this.groupNames.add(group.getName());
	}

	public SecureResource getTable() {
		return table;
	}

	public RoleMapping getMapping() {
		return mapping;
	}

	public List<SecurityGroup> getGroups() {
		return groups;
	}

	public void setHighestMapping(RoleMapping r) {
		if(r.isHigherRoleThan(mapping))
			mapping = r;
	}

	public List<String> getGroupNames() {
		return groupNames;
	}
	
	public void addGroup(SecurityGroup group) {
		this.groups.add(group);
		this.groupNames.add(group.getName());
	}

}
