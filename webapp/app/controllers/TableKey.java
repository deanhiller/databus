package controllers;

public class TableKey {

	private String tableName;
	private Object pkValue;

	public TableKey(String tableName, Object pkValue) {
		this.tableName = tableName;
		this.pkValue = pkValue;
	}

	public String getTableName() {
		return tableName;
	}

	public Object getPkValue() {
		return pkValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pkValue == null) ? 0 : pkValue.hashCode());
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableKey other = (TableKey) obj;
		if (pkValue == null) {
			if (other.pkValue != null)
				return false;
		} else if (!pkValue.equals(other.pkValue))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
	

}
