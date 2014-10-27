package controllers.gui;

import gov.nrel.util.SearchUtils;
import gov.nrel.util.Utility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.DataTypeEnum;
import models.EntityUser;
import models.KeyToTableName;
import models.PermissionType;
import models.SecureSchema;
import models.SecureTable;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.With;
import play.mvc.results.BadRequest;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.SearchPosting;
import controllers.SecurityUtil;
import controllers.api.ApiPostDataPointsImpl;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.ChartUtil;
import controllers.impl.RawTimeSeriesImpl;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.procs.RowMeta;

@With(GuiSecure.class)
public class MyStuff extends Controller {
	
	private static final Logger log = LoggerFactory.getLogger(MyStuff.class);
	
	
	public static void deleteRangeEmpty(String table) {
		if (log.isDebugEnabled())
			log.debug("entering deleteRangeEmpty with table "+table);
		PermissionType permission = SecurityUtil.checkSingleTable2(table);
		if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
			unauthorized("You don't have permission to modify the data in this table");
		String begin="0";
		String finish="0";
		renderTemplate("gui/MyStuff/deleteRange.html", table, begin, finish);
	}
	
	public static void deleteRange(String table, String start, String end) {
		if (log.isDebugEnabled())
			log.debug("entering deleteRange with table "+table+" start: "+start+" end: "+end);
		SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
		PermissionType permission = SecurityUtil.checkSingleTable2(table);
		if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
			unauthorized("You don't have permission to modify the data in this table");
		RawTimeSeriesImpl impl = new RawTimeSeriesImpl();
		
		String timeColumn = targetTable.getPrimaryKey().getColumnName();
		List<String> names = new ArrayList<String>();
		Set<String> keySet = targetTable.getNameToField().keySet();
		for(String n : keySet) {
			names.add(n);
		}
		
		RowMeta rowMeta = new RowMeta(timeColumn, names);
		NoSqlEntityManager mgr = NoSql.em();
		
		impl.init(targetTable.getTableMeta(), Long.valueOf(start), Long.valueOf(end), rowMeta, mgr);
		if (log.isDebugEnabled())
			log.debug("calling deleteRange on Impl");
		impl.deleteRange();
		if (log.isDebugEnabled())
			log.debug("done with deleteRange on Impl");
		render(table);
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
		variablesMap.put("__chartId", "BasicChart-js");
		String encoded = ChartUtil.encodeVariables(variablesMap);
		
		MyChartsGeneric.drawChart(encoded);
	}

	public static void createChart(String table) {
		SecurityUtil.checkSingleTable(table);
		
		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put("title", table+" Last 1000 Data points");
		variablesMap.put("url", "/api/gapV1/rawdataV1/" + table);
		variablesMap.put("_daterangetype", "npoints");
		variablesMap.put("_numberpoints", "2000");
		variablesMap.put("timeColumn", "time");
		variablesMap.put("valueColumn", "value");
		variablesMap.put("yaxisLabel", "units");
		variablesMap.put("units", "units");
		variablesMap.put("__chartId", "BasicChart-js");
		String encoded = ChartUtil.encodeVariables(variablesMap);
		
		MyChartsGeneric.modifyChart(encoded);
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

	public static void postTable(String table, String description, String tag) {
		String action = params.get("submit");
		//pull out duplicate code
	    if ("Save".equals(action)) {
	    	SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
			PermissionType permission = SecurityUtil.checkSingleTable2(table);
			if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
				unauthorized("You don't have permission to change this resource");

			targetTable.setDescription(description);
			NoSql.em().put(targetTable);
			NoSql.em().flush();
			
			flash.success("Your changes were saved");
			tableEdit(table);

	    } else if ("addTag".equals(action)) {
	    	if (StringUtils.isBlank(tag)) {
				flash.success("If you want to add a tag, fill in a value in the 'New Tag' field, then click 'Add Tag'.");
	    		tableEdit(table);
	    	}
	    	SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
			PermissionType permission = SecurityUtil.checkSingleTable2(table);
			if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
				unauthorized("You don't have permission to change this resource");

			targetTable.addTag(tag, NoSql.em());
			boolean itemIndexed = true;
			ArrayList<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
			try {
				SearchUtils.indexTable(targetTable, targetTable.getTableMeta(), solrDocs);
			}
			catch (Exception e) {
				itemIndexed = false;
			}
			SearchPosting.saveSolr("table id = '" + targetTable.getTableMeta().getColumnFamily() + "'", solrDocs, "databusmeta");
			NoSql.em().put(targetTable);
			NoSql.em().flush();
			
			if (itemIndexed)
				flash.success("Your changes were saved");
			else
				flash.success("Your changes were saved, but indexing the searchable information returned an error.  Your item may not appear on search results until the system is reindexed.");

			tableEdit(table);	    
		}  else if (action.startsWith("Remove Tag ")) {
	    	SecureTable targetTable = SecureTable.findByName(NoSql.em(), table);
			PermissionType permission = SecurityUtil.checkSingleTable2(table);
			if(PermissionType.READ_WRITE.isHigherRoleThan(permission))
				unauthorized("You don't have permission to change this resource");

			targetTable.removeTag(action.substring("Remove Tag ".length()), NoSql.em());
			NoSql.em().put(targetTable);
			NoSql.em().flush();
			
			flash.success("Your changes were saved");
			tableEdit(table);	    
		}else {
	      throw new BadRequest("This action is not allowed");
	    }
	}
}
