package controllers;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import models.SecureTable;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.modules2.SqlPullProcessor;

public class SearchPosting {

	private static final Logger log = LoggerFactory.getLogger(SearchPosting.class);
	
	public static void addSolrDataDoc(TypedRow row, SecureTable sdiTable, Collection<SolrInputDocument> solrDocs) {
		if(!sdiTable.isSearchable())
			return;
		
		DboTableMeta table = sdiTable.getTableMeta();

		SolrInputDocument doc = new SolrInputDocument();

		String rowKey = row.getRowKeyString();
		// Add special fields to track the type of record an the primary
		// key.
		doc.addField("id", table.getColumnFamily()+rowKey);

		doc.addField("type", sdiTable.getTableName());
		Set<String> allTermsSet = new HashSet<String>();
		Set<String> parentAllTermsSet = new HashSet<String>();

		Map<String, String> columns = new HashMap<String, String>();
		// Add fields for every column on this row.
		for(TypedColumn col : row.getColumnsAsColl()) {
			String colName = col.getName();
			String fieldValue = col.getValueAsString();
			
			String textFieldName = colName + "_texts";
			doc.addField(textFieldName, fieldValue);
			if (sdiTable.hasSearchableParent())
				columns.put(colName, fieldValue);
			
			if (!colName.matches("description")) {
				String stringFieldName = colName + "_sms";
				doc.addField(stringFieldName, fieldValue);
			}
			
		}
		if (sdiTable.hasSearchableParent()) {
			//we need a prepared statement someday...
			doc.addField("parentId_texts", sdiTable.getSearchableParentTable());
			parentAllTermsSet.add(sdiTable.getSearchableParentTable());
			
			String sql = "select c from "+sdiTable.getSearchableParentTable()+" as c where c."+sdiTable.getSearchableParentParentCol()+"='"+columns.get(sdiTable.getSearchableParentChildCol())+"'";
			if (log.isInfoEnabled()) {
				log.info("searching for the parent table of "+table.getColumnFamily()+" with the sql:");
				log.info(sql);
			}
			QueryResult result = NoSql.em().getTypedSession().createQueryCursor(sql, SqlPullProcessor.BATCH_SIZE);
			Iterable<List<TypedRow>> cursor = result.getAllViewsIter();
			
			List<TypedRow> typedRows = cursor.iterator().next();
			for (TypedRow prow:typedRows) {
				DboTableMeta meta = prow.getView().getTableMeta();
				DboColumnMeta idMeta = meta.getIdColumnMeta();
				if(prow.getRowKey() == null)
					return;
				String parentVal = idMeta.convertTypeToString(prow.getRowKey());
				String parentColName = idMeta.getColumnName();
				System.out.println("---- parent col:"+parentColName+", "+parentVal);
				for(DboColumnMeta colMeta : meta.getAllColumns()) {
					String colName = colMeta.getColumnName();
					TypedColumn column = row.getColumn(colName);
					String strVal = null;
					if(column != null) {
						Object data = column.getValue();
						strVal = colMeta.convertTypeToString(data);
					}
					System.out.println("---- "+colName+", "+strVal);
					SolrInputField field = doc.getField(colName);
					if (field != null)
						doc.addField(colName+"_texts", strVal);
					else
						doc.addField(colName+"_"+sdiTable.getSearchableParentTable()+"_texts", strVal);
					parentAllTermsSet.add(strVal);
				}
			}
			
		}
		
		allTermsSet.addAll(parentAllTermsSet);
		for (String key:doc.getFieldNames()) 
			allTermsSet.add((String)doc.getField(key).getValue());
		doc.addField("allTerms_texts", allTermsSet);

		solrDocs.add(doc);
	}
	
	/*
	 * if 'core' is blank this method will attempt to infer the core by using the value of 
	 * the 'type' field in the first doc in solrDocs.  This won't work for databusmeta!
	 */
	public static void saveSolr(String json, Collection<SolrInputDocument> solrDocs, String core) {
		if(solrDocs.size() == 0)
			return; //nothing to process
		if (StringUtils.isEmpty(core))
			core = (String)solrDocs.iterator().next().getField("type").getValue();
		
		try {
			saveSolrImpl(solrDocs, core);
		} catch (Exception e) {
			if (log.isWarnEnabled())
        		log.warn("indexing on solr failed, need to re-index for json data post="+json+"  solrdocs="+solrDocs, e);
		}
	}

	private static void saveSolrImpl(Collection<SolrInputDocument> solrDocs, String core) throws SolrServerException, IOException,
		ParserConfigurationException, SAXException {
		if (solrDocs != null && !solrDocs.isEmpty() ) {
			UpdateRequest request = new UpdateRequest();
			request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);
			request.add(solrDocs);
			request.process(Search.getSolrCore(core));
		}
	}
}
