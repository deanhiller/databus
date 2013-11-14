package models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

public enum DataTypeEnum {

	TIME_SERIES("timeseries"), RELATIONAL("relational"), RELATIONAL_TIME_SERIES("relationalTimeSeries");
	
	private static final Map<String, DataTypeEnum> types;
	
	static {
		Map<String, DataTypeEnum> typesMap = new HashMap<String, DataTypeEnum>();
		for(DataTypeEnum type : DataTypeEnum.values()) {
			typesMap.put(type.getDbCode(), type);
		} // for
		types = Collections.unmodifiableMap(typesMap);
	} // static
	
	private String dbCode;

	private DataTypeEnum(String dbCode) {
		this.dbCode = dbCode;
	} // DataTypeEnum
	
	public String getDbCode() {
		return dbCode;
	} // getDbCode
	
	public String getPrettyName() {
		return WordUtils.capitalize(dbCode);
	}
	
	public static DataTypeEnum translate(String type) {
		if(type == null) {
			return null;
		} // if
		return types.get(type);
	} // translate
	
} // DataTypeEnum
