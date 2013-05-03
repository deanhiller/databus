package models;

import java.util.ArrayList;
import java.util.List;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.CursorToManyImpl;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;

// This table the user/table pair is unique, should not be duplicated so I would
// normally do a unique constraint here.
@NoSqlEntity
@NoSqlQueries({
	@NoSqlQuery(name="findByName", query="select u from TABLE as u where u.name = :name"),
	@NoSqlQuery(name="findAll", query="select u from TABLE as u"),
	@NoSqlQuery(name="findByKey", query="select s from TABLE as s where s.apiKeyForGet = :key")
})
public class SecurityGroup {
 
	@NoSqlId
	private String id;
	  
	@NoSqlIndexed
	private String name;
	 
	@NoSqlIndexed
	private String apiKeyForGet;
	
	private String apiKeyForPost;
	
	private String description;
	
	@NoSqlOneToMany
	private List<RoleMapping> users = new ArrayList<RoleMapping>();
	
	/**
	 * Some groups have way toooo many tables so we use a cursor instead so we can read in the first
	 * 50 or so, then the next, etc. etc.
	 */
	@NoSqlManyToMany
	private CursorToMany<SecureTable> tablesCursor = new CursorToManyImpl<SecureTable>();
	
	public SecurityGroup() {}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	} // getName

	public void setName(String id) {
		this.name = id;
	} // setName

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getApiKeyForGet() {
		return apiKeyForGet;
	}

	public void setApiKeyForGet(String apiKeyForGet) {
		this.apiKeyForGet = apiKeyForGet;
	}

	public CursorToMany<SecureTable> getTables() {
		return tablesCursor;
	}
	
	public List<RoleMapping> getUsers() {
		return users;
	} // getAdminUsers

	public void addTable(SecureTable t) {
		t.addMapping(this);
		tablesCursor.addElement(t);
	}
	
	public static SecurityGroup findByName(NoSqlEntityManager mgr, String name) {
		Query<SecurityGroup> query = mgr.createNamedQuery(SecurityGroup.class, "findByName");
		query.setParameter("name", name);
		return query.getSingleObject();
	}

	public void addUser(RoleMapping roleMapping) {
		users.add(roleMapping);
	}

	public static Cursor<KeyValue<SecurityGroup>> findAllCursor(NoSqlEntityManager em) {
		Query<SecurityGroup> query = em.createNamedQuery(SecurityGroup.class, "findAll");
		return query.getResults();
	}
	
	public static List<SecurityGroup> findAll2(NoSqlEntityManager em, int firstRow, Integer maxResults) {
		Query<SecurityGroup> query = em.createNamedQuery(SecurityGroup.class, "findAll");
		return query.getResultList(firstRow, maxResults);
	}

	public static SecurityGroup findByKey(NoSqlEntityManager em, String password) {
		Query<SecurityGroup> query = em.createNamedQuery(SecurityGroup.class, "findByKey");
		query.setParameter("key", password);
		return query.getSingleObject();
	}

	public List<SecureTable> getTables(int firstRow, int maxResults) {
		List<SecureTable> tables = new ArrayList<SecureTable>();
		for(int i = 0; i < firstRow && tablesCursor.next(); i++) {
		}
		
		for(int i = 0; i < maxResults && tablesCursor.next(); i++) {
			SecureTable t = tablesCursor.getCurrent();
			//trigger a cache load as playframework is NOT going through getters/setters(if it was, we would NOT need to do this)
			t.getTableName();
			tables.add(t);
		}
		return tables;
	}

	public void setApiKeyForPost(String postKey) {
		apiKeyForPost = postKey;
	}

	public String getApiKeyForPost() {
		return apiKeyForPost;
	}
	
} // SecurityGroup
