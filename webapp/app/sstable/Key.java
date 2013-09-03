package sstable;

public class Key {

	private Object rowKey;
	private String tableName;
	private String keyStr;

	public Key(String virtTableName, Object rowKey, String keyStr) {
		this.tableName = virtTableName;
		this.rowKey = rowKey;
		this.keyStr = keyStr;
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyStr == null) ? 0 : keyStr.hashCode());
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
		Key other = (Key) obj;
		if (keyStr == null) {
			if (other.keyStr != null)
				return false;
		} else if (!keyStr.equals(other.keyStr))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "Key [tableName=" + tableName
				+ ", keyStr=" + keyStr + "]";
	}

	public Object getRowKey() {
		return rowKey;
	}

	public String getTableName() {
		return tableName;
	}

}
