package controllers;

import gov.nrel.util.SearchUtils;
import gov.nrel.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import models.EntityGroup;
import models.EntityUser;
import models.SecureSchema;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import play.mvc.Controller;
import play.mvc.Http.Request;

import com.alvazan.orm.api.base.NoSqlDao;
import com.alvazan.orm.api.z5api.NoSqlSession;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.ScanInfo;
import com.alvazan.orm.api.z8spi.action.IndexColumn;
import com.alvazan.orm.api.z8spi.iter.AbstractCursor;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

public class Admin extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Admin.class);
	
	public static void index() {
    	EntityUser user = Utility.getCurrentUser(session);
    	authCheck(user);
    	
    	List<EntityGroup> groups = EntityGroup.findAll(NoSql.em());
    	List<SecureSchema> schemas = SecureSchema.findAll(NoSql.em());    	
    	if (log.isInfoEnabled())
			log.info("render index. user="+user+" num schemas="+schemas.size()+" groups="+groups.size());
    	
		render(groups, schemas);
	}
	
	public static void sanity() {
    	EntityUser user = Utility.getCurrentUser(session);
    	authCheck(user);
    	
		String username = user.getUsername();
		String password = user.getApiKey();
    	
    	String host = Request.current().host;		
		String baseurl = "//"+host;
    	
		render(username, password, baseurl);
	}

	private static void authCheck(EntityUser user) {
		if(user == null)
			unauthorized("You may have a corrupt session as your user does not exist(developers get this sometimes)");
		else if(!user.isAdmin())
			unauthorized("You are not authorized to view/post this webpage");
	}
	
	public static void users() {
		EntityUser user = Utility.getCurrentUser(session);
		authCheck(user);
		
		List<EntityUser> users = EntityUser.findAll(NoSql.em());
		render(users);
	}
	
	public static void becomeUser(String username) {
		EntityUser user = Utility.getCurrentUser(session);
		authCheck(user);

		EntityUser newUser = NoSql.em().find(EntityUser.class, username);
		session.put("username", username);
		if(user.isAdmin())
			session.put("admin", "true");
		else
			session.remove("admin");
		
		session.put("override", "true");
		
		MyGroups.myGroups();
	}
	
	public static void meta() {
		EntityUser user = Utility.getCurrentUser(session);
		authCheck(user);
		
		Cursor<KeyValue<DboTableMeta>> cursor = NoSqlDao.findAllTables(NoSql.em());
		List<DboTableMeta> tables = new ArrayList<DboTableMeta>();
		int counter = 0;
		while(cursor.next()) {
			tables.add(cursor.getCurrent().getValue());
			counter++;
			if(counter >= 50)
				break;
		}
		
		render(tables);
	}
	
	public static void metaTable(String tableName) {
		EntityUser user = Utility.getCurrentUser(session);
		authCheck(user);
		
		DboTableMeta table = NoSql.em().find(DboTableMeta.class, tableName);
		if(table == null)
			notFound("Column Family does not exist(or at least there is no meta information on this ColumnFamily)");
		DboColumnIdMeta idMeta = table.getIdColumnMeta();
		Collection<DboColumnMeta> columns = table.getAllColumns();
		
		render(idMeta, columns);
	}
	public static void viewIndex(String tableName, String colName) {
		EntityUser user = Utility.getCurrentUser(session);
		authCheck(user);
		
		DboTableMeta table = NoSql.em().find(DboTableMeta.class, tableName);
		if(table == null)
			notFound("Column family="+tableName+" does not exist");
		
		DboColumnMeta col = findColumn(table, colName);
		
		ScanInfo info = ScanInfo.createScanInfo(col, null, null);
		NoSqlSession session = NoSql.em().getSession();
		AbstractCursor<IndexColumn> cursor = session.scanIndex(info, null, null, 100);

		List<Tuple> values = new ArrayList<Tuple>();
		int counter = 0;
		while(cursor.next()) {
			IndexColumn current = cursor.getCurrent();
			byte[] val = current.getIndexedValue();
			byte[] primaryKey = current.getPrimaryKey();
			Object keyObj = table.getIdColumnMeta().convertFromStorage2(primaryKey);
			String keyStr = table.getIdColumnMeta().convertTypeToString(keyObj);
			Object valObj = col.convertFromStorage2(val);
			String valStr = col.convertTypeToString(valObj);
			Tuple t = new Tuple(valStr, keyStr);
			values.add(t);
			counter++;
			if(counter >= 50)
				break;
		}
		
		render(values);
	}

	private static DboColumnMeta findColumn(DboTableMeta table, String name) {
		Collection<DboColumnMeta> columns = table.getAllColumns();
		for(DboColumnMeta m : columns) {
			if(name.equals(m.getColumnName()))
				return m;
		}
		
		if(table.getIdColumnMeta().getColumnName().equals(name))
			return table.getIdColumnMeta();
		
		notFound("Col="+name+" not found on table="+table.getColumnFamily());
		return null;
	}

	public static void reindex() throws IOException, SAXException, ParserConfigurationException, SolrServerException {
		SearchUtils.reindex();
	}
}
