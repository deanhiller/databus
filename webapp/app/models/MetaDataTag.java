package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nosql.util.IterableCursorWrapper;

import org.playorm.cron.impl.db.WebNodeDbo;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.CursorToManyImpl;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;

@NoSqlEntity
@NoSqlQueries({
	@NoSqlQuery(name="findAll", query="select e from TABLE as e"),
	@NoSqlQuery(name="findByName", query="select u from TABLE as u where u.name = :name")
})
public class MetaDataTag {

	@NoSqlId
	@NoSqlIndexed
	private String name;
	
	
	@NoSqlManyToMany
	private CursorToMany<SecureTable> tablesCursor = new CursorToManyImpl<SecureTable>();

	private Integer tableCount = null;
	
	@NoSqlIndexed
	private Integer length = 1;

	public static MetaDataTag getMetaDataTag(String name, Integer length, NoSqlEntityManager mgr) {
		//MetaDataTag result = MetaDataTag.findByName(NoSql.em(), name);
		MetaDataTag result = mgr.find(MetaDataTag.class, name);
		//if (result != null)
		//	System.out.println("found an existing MetaDataTag for name "+name);

		if (result == null) {
			//System.out.println("didn't find an existing MetaDataTag for name "+name);
			result = new MetaDataTag(name, length);
			mgr.fillInWithKey(result);
			mgr.put(result);
		}
		return result;
	}
	
	public static MetaDataTag findByName(NoSqlEntityManager mgr, String name) {
		Query<MetaDataTag> query = mgr.createNamedQuery(MetaDataTag.class, "findByName");
		query.setParameter("name", name);
		return query.getSingleObject();
	}
	
	private MetaDataTag(String name, Integer length) {
		this.name=name;
		this.length=length;
	}
	
	public Iterable<SecureTable> getTables() {
		return new IterableCursorWrapper<SecureTable>(tablesCursor);
	}
	
	public CursorToMany<SecureTable> getTablesCursor() {
		if (tablesCursor == null)
			tablesCursor = new CursorToManyImpl<SecureTable>();
		
		return new RemoveSafeCursor(tablesCursor) {
			protected boolean isDeleted(Object next) {
				return ((SecureTable)next).getPrimaryKey() == null?true:false;
			}
		};
	}
	
	public void addTable(SecureTable t) {
		if(tableCount == null) {
			tableCount = 0;
		}
		
		tableCount++;
		//table is responsible for maintaining his side
		//t.addTag(this);
		getTablesCursor().addElement(t);
	}
	
	//This is gonna be slow...
	public void removeTable(SecureTable t) {

		//long before = System.currentTimeMillis();
		CursorToMany<SecureTable> myTablesCursor = getTablesCursor();
		myTablesCursor.beforeFirst();
		//boolean hasnext = tablesCursor.next();
		while(myTablesCursor.next()) {
			SecureTable current = myTablesCursor.getCurrent();
			current.getTableName();
			if (current.getName().equals(t.getName())) {
				myTablesCursor.removeCurrent();
				tableCount--;
				break;
			}
		}
		myTablesCursor.beforeFirst();
		//long after = System.currentTimeMillis();
		//System.out.println("   **** time for loop in removeTable: "+(after-before));
	}
	
	public Integer getTableCount() {
		return tableCount==null?0:tableCount;
	}
	
//	public void populateTableCount(NoSqlEntityManager mgr) {
//		if(tableCount == null) {
//			CursorToMany<SecureTable> c = getTablesCursor();
//			c.beforeFirst();
//			tableCount = 0;
//			while(c.next()) {
//				tableCount++;
//			}
//			c.beforeFirst();
//			
//			mgr.put(this);
//			mgr.flush();
//		}
//	}

	
	public Integer getLength() {
		return length;
	}


	public void setLength(Integer length) {
		this.length = length;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return getName()+" length "+getLength()+", "+super.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass()))
			return false;
		if (((MetaDataTag)other).getName().equals(getName()) && ((MetaDataTag)other).getLength().equals(getLength()))
			return true;
		return false;
	}

	//this is a bit of a hack right now, with the string matching, but we don't even know 
	//if 'remove' will ever be a requirement yet.  This is just for testing.  Soooo... KISS
	public boolean containsSubtag(MetaDataTag tagobj) {
		if (this.getName().contains(SecureTable.tagDelimiter+tagobj.getName()+SecureTable.tagDelimiter) ||
				this.getName().startsWith(tagobj.getName()+SecureTable.tagDelimiter) ||
				this.getName().endsWith(SecureTable.tagDelimiter+tagobj.getName()) ||
				this.getName().equals(tagobj.getName()))
			return true;
		else return false;
	}
	
}
