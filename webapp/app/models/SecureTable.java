package models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.LocalDateTime;
import org.mortbay.log.Log;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlOneToOne;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

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

//	@Deprecated
//	@NoSqlOneToMany
//	private List<SecurityGroup> owningGroups = new ArrayList<SecurityGroup>();
	
	@NoSqlManyToOne
	private SecureSchema schema;
	
	private String typeOfData = DataTypeEnum.TIME_SERIES.getDbCode();
	
	@NoSqlIndexed
	private Boolean isSearchable;
	
	private String searchableParentTable;
	
	private String searchableParentChildCol;
	
	private String searchableParentParentCol;
	
	private String facetFields="";

	@NoSqlOneToOne
	private DboTableMeta tableMeta;
	
	@NoSqlManyToMany
	private List<MetaDataTag> tags = new ArrayList<MetaDataTag>();
	
	private Boolean isForLogging;
	
	public static final transient String tagDelimiter = "_:_";

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
		return null;
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
	
	
	public void addTag(String tagname, NoSqlEntityManager em) {

		//this version of this code uses the 'setTags()' method to exersise that code.  it's slow.
//		MetaDataTag tagobj = MetaDataTag.getMetaDataTag(tag, 1, em);
//		List<MetaDataTag> currentFirstLevelTags = getTags();
//		if (!currentFirstLevelTags.contains(tagobj)) {
//			currentFirstLevelTags.add(tagobj);
//		}
//		setTags(currentFirstLevelTags, em);
		
		MetaDataTag tagobj = MetaDataTag.getMetaDataTag(tagname, 1, em);
		if (tags.contains(tagobj))
			return;
		TreeSet<MetaDataTag>tagSet = new TreeSet<MetaDataTag>(new MetaDataTagComparator());
		tagSet.addAll(getTags());
		tagSet.add(tagobj);
		Set<MetaDataTag> allTagCombos = permute(tagSet, em);
		for (MetaDataTag tag:allTagCombos) {
			if (!tags.contains(tag)) {
				tags.add(tag);
				tag.addTable(this);
				em.put(tag);
			}	
		}
		
	}
	
	public void removeTag(String tagToRemove, NoSqlEntityManager em) {
		MetaDataTag tagobj = MetaDataTag.getMetaDataTag(tagToRemove, 1, em);
		if (tags.contains(tagobj)) {
			//need to remove not just this tag, but all tag combinations that include it.
			long before = System.currentTimeMillis();

			for(int i = tags.size()-1; i >= 0; i--) {
				MetaDataTag thistag = tags.get(i);
				if (thistag.containsSubtag(tagobj)) {
					tags.remove(i);
					thistag.removeTable(this);
					if (thistag.getTableCount()==0)
						em.remove(thistag);
					else
						em.put(thistag);

				}
				long after = System.currentTimeMillis();
				em.put(this);
				after = System.currentTimeMillis();
			}
		}
	}
	
	
	public void setTags(List<MetaDataTag> newtags, NoSqlEntityManager mgr) {
		
		TreeSet<MetaDataTag>tagSet = new TreeSet<MetaDataTag>(new MetaDataTagComparator());
		tagSet.addAll(newtags);
		Set<MetaDataTag> allTagCombos = permute(tagSet, mgr);
		long before = System.currentTimeMillis();
		
		for(int i = tags.size()-1; i >= 0; i--) {
			MetaDataTag tag = tags.get(i);
			if (!allTagCombos.contains(tag)) {
				//remove me from the tags tables
				tag.removeTable(this);
				//remove this tag from my list of tags
				tags.remove(i);
				//delete the tag if empty???  only if empty and not in provided tags list
				if (tag.getTableCount()==0)
					mgr.remove(tag);
			}
			mgr.put(tag);
			//mgr.flush();
		}
		long after = System.currentTimeMillis();
		before = System.currentTimeMillis();

		for (MetaDataTag tag:allTagCombos) {
			if (!tags.contains(tag)) {
				tags.add(tag);
				tag.addTable(this);
				mgr.put(tag);
			}
					
		}
		after = System.currentTimeMillis();
	}

	
	
	private Set<MetaDataTag> permute(TreeSet<MetaDataTag> newtags, NoSqlEntityManager mgr)
	  {
		TreeSet<MetaDataTag> result = new TreeSet<MetaDataTag>(new MetaDataTagComparator());
		if (newtags == null || newtags.size() == 0)
			return result;

	    // Termination condition: only 1 permutation for a list of length 1
		if (newtags.size() == 1) {
			result.add(MetaDataTag.getMetaDataTag(newtags.first().getName(), newtags.first().getLength(), mgr));
		} else {
			TreeSet<MetaDataTag> subtags = new TreeSet<MetaDataTag>(new MetaDataTagComparator());
			MetaDataTag first = newtags.first();
			subtags.addAll(newtags);
			subtags.remove(first);
			Set<MetaDataTag> subtagPermutations = permute(subtags, mgr);
			result.addAll(subtagPermutations);
			
			result.add(MetaDataTag.getMetaDataTag(first.getName(), first.getLength(), mgr));
			for (MetaDataTag subtag : subtagPermutations) {
				MetaDataTag thistag = MetaDataTag.getMetaDataTag(first.getName()+tagDelimiter+subtag.getName(), subtag.getLength()+1, mgr);
				result.add(thistag);
			}
			
		}
	    return result;
	  }

	public List<MetaDataTag> getTags() {
		List<MetaDataTag> result = new ArrayList<MetaDataTag>();
		for (MetaDataTag tag:tags){
			if (tag.getLength() == 1)
				result.add(tag);
		}
		return result;
	}
	
	public List<MetaDataTag> getTagsIncludingCalculated() {
		return tags;
	}
	
	private static class MetaDataTagComparator implements Comparator<MetaDataTag> {

	    @Override
	    public int compare(MetaDataTag o1, MetaDataTag o2) {
	        if(o1.getName() != null && o2.getName() != null){
	            return o1.getName().compareTo(o2.getName());
	        }

	        return 0;
	    }

	}

} // Table
