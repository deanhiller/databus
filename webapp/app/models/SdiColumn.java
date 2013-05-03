package models;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;

@NoSqlEntity
public class SdiColumn {

	@NoSqlId
	private String id;
	
	private String semanticType;

	private String columnName;
	
	@NoSqlOneToMany(keyFieldForMap="name")
	private Map<String, SdiColumnProperty> properties = new HashMap<String, SdiColumnProperty>();
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSemanticType() {
		return semanticType;
	}

	public void setSemanticType(String semanticType) {
		this.semanticType = semanticType;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public Map<String, SdiColumnProperty> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, SdiColumnProperty> properties) {
		this.properties = properties;
	}
	
} // Column
