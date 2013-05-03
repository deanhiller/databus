package models;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum PermissionType {

	READ("read", 15), READ_WRITE("readwrite", 10), ADMIN("admin", 5);
	
	private static final Map<String, PermissionType> valueToType;

	static {
		Map<String, PermissionType> valueToTypeMap = new HashMap<String, PermissionType>();
		for(PermissionType type : PermissionType.values()) {
			valueToTypeMap.put(type.value(), type);
		} // for
		valueToType = Collections.unmodifiableMap(valueToTypeMap);
	} // static
	
	private String value;
	private int level;
	
	private PermissionType(String dbValue, int level) {
		this.value = dbValue;
		this.level = level;
	} // PermissionType
	
	public String value() {
		return value;
	} // value
	
	public static PermissionType lookup(String dbValue) {
		return valueToType.get(dbValue);
	} // lookup

	public boolean isHigherRoleThan(PermissionType type) {
		if(this.level < type.level)
			return true;
		return false;
	}

	public static Set<PermissionType> allRoles() {
		Set<PermissionType> types = new HashSet<PermissionType>();
		for(PermissionType t : valueToType.values()) {
			types.add(t);
		}
		return types;
	}

	
} // PermissionType
