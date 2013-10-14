package controllers.gui;

import gov.nrel.util.Utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.DataTypeEnum;
import models.EntityUser;
import models.KeyToTableName;
import models.PermissionType;
import models.RoleMapping;
import models.SecureResource;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;

import controllers.SecurityUtil;
import controllers.TableInfo;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.ChartUtil;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Http.Request;

@With(GuiSecure.class)
public class MyStuff extends Controller {
	
	private static final Logger log = LoggerFactory.getLogger(MyStuff.class);
	
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
		SecureTable tab = SecurityUtil.checkSingleTable(table);
		
		if(tab.getTypeOfData() == DataTypeEnum.RELATIONAL)
			redirect("/api/csv/firstvaluesV1/1000/getdataV1/select+t+from+"+table+"+as+t");
		else
			redirect("/api/csv/firstvaluesV1/1000/rawdataV1/"+table+"?reverse=true");
	}

	public static void tableJson(String table) {
		SecureTable tab = SecurityUtil.checkSingleTable(table);
		
		if(tab.getTypeOfData() == DataTypeEnum.RELATIONAL)
			redirect("/api/firstvaluesV1/1000/getdataV1/select+t+from+"+table+"+as+t");
		else
			redirect("/api/firstvaluesV1/1000/rawdataV1/"+table+"?reverse=true");
	}
	
	public static void tableChart(String table) {
		SecurityUtil.checkSingleTable(table);
		
		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put("title", table+" Last 2000 Data points");
		variablesMap.put("url", "/api/gapV1/rawdataV1/" + table);
		variablesMap.put("_daterangetype", "npoints");
		variablesMap.put("_numberpoints", "2000");
		variablesMap.put("timeColumn", "time");
		variablesMap.put("valueColumn", "value");
		variablesMap.put("yaxisLabel", "units");
		variablesMap.put("units", "units");
		String encoded = ChartUtil.encodeVariables(variablesMap);
		String chartId = "BasicChart-js";
		
		MyChartsGeneric.drawChart(chartId, encoded);
	}

	public static void createChart(String table) {
		SecurityUtil.checkSingleTable(table);
		
		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put("title", table+" Last 1000 Data points");
		variablesMap.put("url", "/api/firstvaluesV1/1000/rawdataV1/" + table + "?reverse=true");
		variablesMap.put("timeColumn", "time");
		variablesMap.put("valueColumn", "value");
		variablesMap.put("yaxisLabel", "units");
		variablesMap.put("units", "units");
		String encoded = ChartUtil.encodeVariables(variablesMap);
		String chartId = "BasicChart-js";
		
		MyChartsGeneric.modifyChart(chartId, encoded);
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

	public static void postTable(String table, String description) {
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		PermissionType permission = SecurityUtil.checkSingleTable2(table);
		if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
			unauthorized("You don't have permission to change this resource");

		targetTable.setDescription(description);
		NoSql.em().put(targetTable);
		NoSql.em().flush();
		
		flash.success("Your changes were saved");
		tableEdit(table);
	}
}
