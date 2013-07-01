package controllers.gui;

import gov.nrel.util.Utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.EntityUser;
import models.KeyToTableName;
import models.RoleMapping;
import models.SecureResource;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;

import controllers.TableInfo;
import controllers.gui.auth.GuiSecure;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Http.Request;

@With(GuiSecure.class)
public class MyStuff extends Controller {
	
	private static final Logger log = LoggerFactory.getLogger(MyStuff.class);
	
	public static void myTables() {
		EntityUser user = Utility.getCurrentUser(session);
		Map<String, TableInfo> loadedTables = new HashMap<String, TableInfo>();
		
		int counter = 0;
		List<RoleMapping> groups = user.getGroups();
		for(RoleMapping r : groups) {
			SecurityGroup group = r.getGroup();
			if (log.isInfoEnabled())
				log.info("checking group="+group.getName());
			CursorToMany<SecureTable> tables = group.getTables();
			while(tables.next() && counter < 50) {
				SecureTable t = tables.getCurrent();
				if (log.isInfoEnabled())
					log.info("checking table="+t.getTableName());
				TableInfo info = loadedTables.get(t.getId());
				if(info == null) {
					if (log.isInfoEnabled())
						log.info("no other relationship with table="+t.getTableName()+", adding");
					loadedTables.put(t.getId(), new TableInfo(t, group, r));
					counter++;
				} else {
					if (log.isInfoEnabled())
						log.info("already related to table though another group..adding group="+group.getId());
					info.setHighestMapping(r);
					info.addGroup(group);
				}
			}
		}

		Collection<TableInfo> infos = loadedTables.values();
		render(infos);
	}
	
	public static void tableEdit(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		
		if (targetTable == null) {
			notFound("Page not found");
		}
		if (log.isInfoEnabled())
			log.info("loaded the table "+targetTable.getName()+" searchable="+targetTable.isSearchable());
		
		render(group, targetTable);
	}
	
	private static SecureSchema authTableCheck(String table, EntityUser user) {
		NoSqlEntityManager em = NoSql.em();
		SecureTable sdiTable = SecureTable.findByName(em, table);
		if(sdiTable == null)
			notFound();
		
		String rowKey = KeyToTableName.formKey(table, user.getUsername(), user.getApiKey());
		KeyToTableName info = em.find(KeyToTableName.class, rowKey);
		if (info != null || user.isAdmin()) {
			return sdiTable.getSchema();
		}
		
		return null;
	}
	public static void tableDataset(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		
		redirect("/api/csv/firstvaluesV1/1000/rawdataV1/"+table+"?reverse=true");
	}

	public static void tableJson(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		
		redirect("/api/firstvaluesV1/1000/rawdataV1/"+table+"?reverse=true");
	}
	
	public static void tableChart(String table) {
		EntityUser user = Utility.getCurrentUser(session);
		SecureSchema group = authTableCheck(table, user);
		if(group == null)
			unauthorized("You have no access to this table");
		String username = user.getUsername();
		String password = user.getApiKey();
		String host = Request.current().host;		
		String baseurl = "//"+host;
		
		SecureResource targetTable = SecureTable.findByName(NoSql.em(), table);
		if (targetTable == null) {
			notFound("Page not found");
		}
		
		render(username, password, baseurl, targetTable);
	}
	
	public static void aggData(String aggregationName) {
		EntityUser user = Utility.getCurrentUser(session);

		String username = user.getUsername();
		String password = user.getApiKey();
		String host = Request.current().host;		
		String baseurl = "//"+host;
		
		//check to make sure this agg actually exists
//		SecureResource targetTable = SecureTable.findByName(NoSql.em(), table);
//		if (targetTable == null) {
//			notFound("Page not found");
//		}
		String agg = aggregationName;
		render(username, password, baseurl, agg);
	}
}
