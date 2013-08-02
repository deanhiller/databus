package controllers.gui.util;

import models.PermissionType;
import models.SecureResourceGroupXref;

public class AccessInfo {

	private String accessMethod;
	private SecureResourceGroupXref ref;

	public AccessInfo(String accessMethod, SecureResourceGroupXref ref) {
		this.accessMethod = accessMethod;
		this.ref = ref;
	}
	
	public PermissionType getPermission() {
		return ref.getPermission();
	}
	
	public String getResourceName() {
		return ref.getResource().getName();
	}
	
	public String getAccessMethod() {
		return accessMethod;
	}

}
