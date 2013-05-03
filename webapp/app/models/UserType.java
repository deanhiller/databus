package models;

import java.util.HashMap;
import java.util.Map;

public enum UserType {

	ROBOT("robot"), USER("user"), GROUP("group");
	
	private static final Map<String, UserType> vals = new HashMap<String, UserType>();
	static {
		for(UserType t : UserType.values()) {
			vals.put(t.getDbValue(), t);
		}
	}
	
	private String dbValue;

	private UserType(String val) {
		this.dbValue = val;
	}
	
	public String getDbValue() {
		return dbValue;
	}
	
	public static UserType lookup(String val) {
		if(val == null)
			return UserType.USER;
		return vals.get(val);
	}
	
}
