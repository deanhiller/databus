package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDateTime;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlOneToOne;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

@NoSqlQueries({
	@NoSqlQuery(name = "findByName", query = "select t from TABLE as t where t.tableName = :name"),
	@NoSqlQuery(name="findAll", query="select u from TABLE as u")
})
@NoSqlDiscriminatorColumn("sdiTable")
public class SecureTable extends SecureResource {

	@NoSqlOneToMany(keyFieldForMap = "columnName")
	private Map<String, SdiColumn> nameToField = new HashMap<String, SdiColumn>();

	@NoSqlIndexed
	private String tableName;

	private String classType = "sdiTable";
	
	// TODO: Want a special kind of == only index here...create that in the ORM
	// layer so we can do very very quick queries AND remove the KeyToTableName
	// entity but keep
	// the KeyToTableName column family as a lookup table that is very fast.
	private String secureKeyToPostData;

	private String description;
	
	@NoSqlOneToOne
	private SdiColumn primaryKey;

	private LocalDateTime dateCreated;

	@Deprecated
	@NoSqlOneToMany
	private List<SecurityGroup> owningGroups = new ArrayList<SecurityGroup>();
	
	@NoSqlManyToOne
	private SecureSchema schema;
	
	private String typeOfData = DataTypeEnum.TIME_SERIES.getDbCode();
	
	private Boolean isSearchable;
	
	private String searchableParentTable;
	
	private String searchableParentChildCol;
	
	private String searchableParentParentCol;
	
	private String facetFields="";

	@NoSqlOneToOne
	private DboTableMeta tableMeta;

	private Boolean isForLogging;

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public String getTableName() {
		return tableName;
	} // getTableName

	public void setTableName(String tableName) {
		this.tableName = tableName;
	} // setTableName

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getDateCreated() {
		return dateCreated;
	} // getDateCreated

	public void setDateCreated(LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	} // setDateCreated

	public void addMapping(SecurityGroup userSide) {
		owningGroups.add(userSide);
	} // addMapping
	
	public void setSchema(SecureSchema theSchema) {
		schema = theSchema;
	} 

	public DboTableMeta getTableMeta() {
		return tableMeta;
	}

	public void setTableMeta(DboTableMeta tableMeta) {
		this.tableMeta = tableMeta;
	}

	public static SecureTable findByName(NoSqlEntityManager mgr, String tableName) {
		Query<SecureTable> query = mgr.createNamedQuery(SecureTable.class, "findByName");
		query.setParameter("name", tableName);
		return query.getSingleObject();
	}

	public Map<String, SdiColumn> getNameToField() {
		return nameToField;
	}

	public void setNameToField(Map<String, SdiColumn> nameToField) {
		this.nameToField = nameToField;
	}

	public SdiColumn getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(SdiColumn primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Deprecated
	public String getSecureKeyToPostData() {
		return secureKeyToPostData;
	}

	@Deprecated
	public void setSecureKeyToPostData(String secureKeyToPostData) {
		this.secureKeyToPostData = secureKeyToPostData;
	}

	public DataTypeEnum getTypeOfData() {
		return DataTypeEnum.translate(typeOfData);
	} // getTypeOfData

	public void setTypeOfData(DataTypeEnum type) {
		if (type == null) {
			throw new IllegalArgumentException(
					"Can not set type to null.  It must be a valid type of data");
		} // if
		typeOfData = type.getDbCode();
	} // setTypeOfData

	public Boolean isSearchable() {
		if(isSearchable == null)
			return false;
		return isSearchable;
	}
	
	public void setSearchable(Boolean searchable) {
		this.isSearchable = searchable;
	}
	
	public List<SecurityGroup> getOwningGroups() {
		return owningGroups;
	}

	public static Cursor<KeyValue<SecureTable>> findAllCursor(NoSqlEntityManager em) {
		Query<SecureTable> query = em.createNamedQuery(SecureTable.class, "findAll");
		return query.getResults("tableName");
	}
	
	public static List<SecureTable> findAll(NoSqlEntityManager em, int rowNum, int maxResult) {
		Cursor<KeyValue<SecureTable>> cursor = findAllCursor(em);
		List<SecureTable> users = new ArrayList<SecureTable>();
		int counter = -1;
		while(cursor.next()) {
			counter++;
			if(counter < rowNum) {
				continue;
			}
			KeyValue<SecureTable> kv = cursor.getCurrent();
			if(kv.getValue() == null)
				continue;
			users.add(kv.getValue());
			if(counter >= rowNum+maxResult)
				break;
		}
		return users;
	}

	@Override
	public String getName() {
		return tableName;
	}

	public SecureSchema getSchema() {
		return schema;
	}

	public String getFacetFields() {
		return facetFields;
	}

	public void setFacetFields(String facetFields) {
		this.facetFields = facetFields;
	}

	public boolean hasSearchableParent() {
		return (searchableParentTable!=null && searchableParentTable.length()!=0);
	}
	
	public String getSearchableParentTable() {
		return searchableParentTable;
	}

	public void setSearchableParentTable(String searchableParentTable) {
		this.searchableParentTable = searchableParentTable;
	}

	public String getSearchableParentChildCol() {
		return searchableParentChildCol;
	}

	public void setSearchableParentChildCol(String searchableParentChildCol) {
		this.searchableParentChildCol = searchableParentChildCol;
	}

	public String getSearchableParentParentCol() {
		return searchableParentParentCol;
	}

	public void setSearchableParentParentCol(String searchableParentParentCol) {
		this.searchableParentParentCol = searchableParentParentCol;
	}

	public boolean isForLogging() {
		if(isForLogging == null)
			return false;
		return isForLogging;
	}

	public void setIsForLogging(Boolean isForLogging) {
		this.isForLogging = isForLogging;
	}

} // Table
