package models;

import java.util.ArrayList;
import java.util.List;

import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlOneToOne;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

@NoSqlEntity
public class KeyToTableName {

	@NoSqlId(usegenerator=false)
	private String key;

	@NoSqlOneToOne
	private DboTableMeta tableMeta;

	@NoSqlOneToOne
	private SecureTable sdiTableMeta; 
	
	@NoSqlEmbedded
	private List<String> permission = new ArrayList<String>();
	
	public KeyToTableName() {}
	public KeyToTableName(String key2, SecureTable table, PermissionType perm) {
		this.key = key2;
		this.sdiTableMeta = table;
		this.tableMeta = table.getTableMeta();
		addPermission(perm);
	}

	public String getKey() {
		return key;
	} // getKey

	public void setKey(String key) {
		this.key = key;
	} // setKey

	public DboTableMeta getTableMeta() {
		return tableMeta;
	} // getTableMeta

	public void setTableMeta(DboTableMeta tableMeta) {
		this.tableMeta = tableMeta;
	} // setTableMeta

	public SecureTable getSdiTableMeta() {
		return sdiTableMeta;
	}

	public void setSdiTableMeta(SecureTable sdiTableMeta) {
		this.sdiTableMeta = sdiTableMeta;
	}

	@Deprecated
	public static String formKey(String tableName, String key2) {
		return tableName+key2;
	}
	public static String formKey(String tableName, String user, String key2) {
		return tableName+user+key2;
	}
	
	public void addPermission(PermissionType p) {
		permission.add(p.value());
	}
	public PermissionType getHighestPermission() {
		if(permission.size() == 0)
			return PermissionType.READ_WRITE; //before this table only had read write keys so if null, the permission is read/write.
		else if(permission.contains(PermissionType.ADMIN.value()))
			return PermissionType.ADMIN;
		else if(permission.contains(PermissionType.READ_WRITE.value()))
			return PermissionType.READ_WRITE;
		
		return PermissionType.READ;
	}
	
} // KeyToTableName
