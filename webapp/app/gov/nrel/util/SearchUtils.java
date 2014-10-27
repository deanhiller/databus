package gov.nrel.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import models.MetaDataTag;
import models.SdiColumn;
import models.SecureSchema;
import models.SecureTable;
import models.StreamAggregation;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import play.Play;
import play.mvc.Util;

import com.alvazan.orm.api.exc.RowNotFoundException;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;

import controllers.Search;
import controllers.SearchPosting;
import controllers.modules2.SqlPullProcessor;

public class SearchUtils {

	private static final Logger log = LoggerFactory.getLogger(SearchUtils.class);
	private static final int REINDEX_BATCH_SIZE = 500;
	private static Map<String, String> knownCores = new HashMap<String, String>();
	
	public static boolean solrIsActivated() {
		String prop = Play.configuration.getProperty("solr.mode");
		return !"off".equals(prop);
	}
	
	public static void indexAggregation(StreamAggregation aggregation, SecureSchema owningSchema) throws IOException, SAXException, ParserConfigurationException, SolrServerException {
		// Solr server instance
		SolrServer solrServer = Search.getSolrServer();
		if (solrServer == null)
			return;
		// Add this new table to the meta index...
		createCoreIfNeeded("databusmeta", "databusmeta", solrServer);
		SolrInputDocument doc = new SolrInputDocument();

		// Add special fields to track the type of record an the primary key.
		doc.addField("id", aggregation.getName());
		doc.addField("type", "aggregation");
		//aggregations don't store their creator.  Add something like this later
		//doc.addField("creator_texts", aggregation.getCreator().getUsername());
		
		
		doc.addField("description_texts", aggregation.getUrls());

		//There is a bug in the 2 way relationship between aggs and schemas, right now it's possible for a 
		//schema to have an aggregation in it's 'getAggregations()' list, but the aggregation DOES NOT HAVE 
		//that schema set as it's .getSchema().  It's possible to pass in the owning schema to account for this.
		if (aggregation.getSchema() != null) {
			doc.addField("database_texts", aggregation.getSchema().getSchemaName());
			doc.addField("databaseDescription_texts", aggregation.getSchema().getDescription());
		}
		else if (owningSchema != null) {
			doc.addField("database_texts", owningSchema.getSchemaName());
			doc.addField("databaseDescription_texts", owningSchema.getDescription());
		}


		Set<String> allTermsSet = new HashSet<String>();

		allTermsSet.add((String) doc.getField("id").getValue());
		allTermsSet.add((String) doc.getField("type").getValue());
		allTermsSet.addAll(aggregation.getUrls());

		if (aggregation.getSchema() != null && owningSchema != null) {
			allTermsSet.add((String) doc.getField("database_texts").getValue());
			allTermsSet.add((String) doc.getField("databaseDescription_texts").getValue());
		}

		doc.addField("allTerms_texts", allTermsSet);

		UpdateRequest request = new UpdateRequest();
		request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);
		request.add(doc);
		request.process(Search.getSolrCore("databusmeta"));
	}
	
	
	public static void indexSchema(SecureSchema theSchema)
			throws IOException, SAXException, ParserConfigurationException,
			SolrServerException {
		// Solr server instance
		SolrServer solrServer = Search.getSolrServer();
		if (solrServer == null)
			return;
		// Add this new table to the meta index...
		createCoreIfNeeded("databusmeta", "databusmeta", solrServer);
		SolrInputDocument doc = new SolrInputDocument();

		// Add special fields to track the type of record an the primary
		// key.
		doc.addField("id", theSchema.getSchemaName());
		doc.addField("type", "database");
		if (theSchema.getCreator() != null)
			doc.addField("creator_texts", theSchema.getCreator().getUsername());
		doc.addField("description_texts", theSchema.getDescription());
		doc.addField("database_texts", theSchema.getSchemaName());
		doc.addField("databaseDescription_texts", theSchema.getDescription());


		Set<String> allTermsSet = new HashSet<String>();

		allTermsSet.add((String) doc.getField("id").getValue());
		allTermsSet.add((String) doc.getField("type").getValue());
		allTermsSet.add((String) doc.getField("description_texts").getValue());

		allTermsSet.add((String) doc.getField("database_texts").getValue());

		doc.addField("allTerms_texts", allTermsSet);

		UpdateRequest request = new UpdateRequest();
		request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, false);
		request.add(doc);
		request.process(Search.getSolrCore("databusmeta"));
		List<StreamAggregation> aggregations = theSchema.getAggregations();
		for (StreamAggregation agg: aggregations) {
			indexAggregation(agg, theSchema);
		}
	}
	

	public static void unindexTable(SecureTable t) throws IOException, ParserConfigurationException, SAXException, SolrServerException {
		SolrServer solrServer = Search.getSolrServer();
		if (solrServer == null)
			return;
		
		UpdateRequest request = new UpdateRequest();
		request.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
		request.deleteById(t.getTableName());
		request.process(Search.getSolrCore("databusmeta"));
		
		if (t.isSearchable()) {
			boolean coreExists = true;
			try {
				CoreAdminResponse statusResponse = CoreAdminRequest.getStatus(
					t.getTableName(), solrServer);
				coreExists = statusResponse.getCoreStatus(t.getTableName()).size() > 0;
			}
			catch (Exception e) {
				coreExists = false;
			}
			
			if (coreExists) {
				CoreAdminRequest.Unload deleteReq = new CoreAdminRequest.Unload(true);
				deleteReq.setCoreName(t.getTableName());
				deleteReq.process(solrServer);
			}
			
			
			
		}
		
	}
	
	public static void indexTable(SecureTable t, DboTableMeta tableMeta, Collection<SolrInputDocument> solrDocs)
			throws IOException, SAXException, ParserConfigurationException,
			SolrServerException {
		
		// Solr server instance
		SolrServer solrServer = Search.getSolrServer();
		if (solrServer == null)
			return;
		// Add this new table to the meta index...
		createCoreIfNeeded("databusmeta", "databusmeta", solrServer);
		SolrInputDocument doc = new SolrInputDocument();

		// Add special fields to track the type of record an the primary
		// key.
		doc.addField("id", t.getTableName());
		doc.addField("type", "table");
		if (t.getCreator() != null)
			doc.addField("creator_texts", t.getCreator().getUsername());
		doc.addField("description_texts", t.getDescription());
		if (t.getSchema() != null) {
			doc.addField("database_texts", t.getSchema().getSchemaName());
			doc.addField("databaseDescription_texts", t.getSchema()
					.getDescription());
		}
		if (t.isSearchable())
			doc.addField("isSearchable_texts", "true");
		else
			doc.addField("isSearchable_texts", "false");
		
		Set<String> allTagsSet = new HashSet<String>();
		if (t.getTags() != null && t.getTags().size() >0) {
			for (MetaDataTag m : t.getTags()) {
				if (m.getLength().equals(1))
					allTagsSet.add(m.getName());
			}
		}
		doc.addField("tag_texts", allTagsSet);
		doc.addField("tag_sms", allTagsSet);
		if (t.isFullyGeolocated())
			doc.addField("location", ""+t.getLat()+","+t.getLon());
			

		Set<String> allTermsSet = new HashSet<String>();
		Set<String> columnsSet = new HashSet<String>();

		for (DboColumnMeta m : tableMeta.getAllColumns()) {
			SdiColumn sdicol = t.getNameToField().get(m.getColumnName());
			columnsSet.add(sdicol.getColumnName());
		}

		doc.addField("column_texts", columnsSet);

		allTermsSet.add((String) doc.getField("id").getValue());
		allTermsSet.add((String) doc.getField("type").getValue());
		allTermsSet.add((String) doc.getField("description_texts").getValue());

		allTermsSet.addAll(columnsSet);
		allTermsSet.addAll(allTagsSet);
		
		if (doc.getField("database_texts") != null) {
			allTermsSet.add((String) doc.getField("database_texts").getValue());
			allTermsSet.add((String) doc.getField("databaseDescription_texts").getValue());
		}
		doc.addField("allTerms_texts", allTermsSet);

		solrDocs.add(doc);
		// Create the core for data from this table to be indexed into when data is posted to this table
		if (t.isSearchable())
			createCoreIfNeeded(t.getTableName(), t.getTableName(), solrServer);
	}

	private static boolean createCoreIfNeeded(String coreName, String dataDir,
			SolrServer server) throws IOException, SolrServerException {
		boolean coreExists = true;
		//this is just a performance optimization for the reindex to prevent it from testing if a 
		//core exists each time it tries to write to it.  Cache known cores:
		if (knownCores.get(coreName) != null)
			return true;

		// SolrJ provides no direct method to check if a core exists, but
		// getStatus will return an empty list for any core that doesn't.
		try {
			CoreAdminResponse statusResponse = CoreAdminRequest.getStatus(
					coreName, server);
			coreExists = statusResponse.getCoreStatus(coreName).size() > 0;
		} catch (SolrException se) {
			coreExists = false;
		}

		if (!coreExists) {
			// Create the core
			if (log.isInfoEnabled())
				log.info("Creating Solr core: " + coreName);
			CoreAdminRequest.Create create = new CoreAdminRequest.Create();
			create.setCoreName(coreName);
			create.setInstanceDir(coreName);
			create.setDataDir(dataDir);
			create.process(server);
		}
		knownCores.put(coreName, coreName);

		return !coreExists;
	}
	
	@Util
	public static void reindex() throws IOException, SAXException, ParserConfigurationException, SolrServerException {
		
		deleteExistingCores();
		indexSchemas();
		
		Cursor<KeyValue<SecureTable>> tablesCursor = SecureTable.findAllCursor(NoSql.em());
		int i = 0;
		//very important to ignore any error and continue indexing.  If an index has gotten 
		//corrupted (which happens...) this will throw com.alvazan.orm.api.exc.RowNotFoundException
		//and will kill the entire reindex.
		long docsindexed = 0;
		long startTime = System.currentTimeMillis()-1;
		Collection<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
		while(tablesCursor.next()) {
			if (++i%200==0)
				NoSql.em().clear();
			KeyValue<SecureTable> kv = tablesCursor.getCurrent();
			
			try {
				if(kv.getValue() == null)
					continue;
			}
			catch (RowNotFoundException rnfe) {
				if (log.isInfoEnabled())
					log.error("got a corrupt index while reindexing, ignoring the error and continuing with indexing of other data.");
				//rnfe.printStackTrace();
				continue;
			}
			SecureTable table = kv.getValue();
			DboTableMeta meta = table.getTableMeta();
			
			SearchUtils.indexTable(table, meta, solrDocs);
			
			if (table.isSearchable()) {
				
				log.info("found a searchable table "+table.getName()+" indexing it.");
				String sql = "select c from "+table.getTableName()+" as c";
				Collection<SolrInputDocument> tablesolrDocs = new ArrayList<SolrInputDocument>();


				try {
					QueryResult result = NoSql.em().getTypedSession().createQueryCursor(sql, SqlPullProcessor.BATCH_SIZE);
					Iterator<List<TypedRow>> cursor = result.getAllViewsIter().iterator();
					
					while (true) {
						//I hate this, but cursor.hasNext() can throw an exception which means we need to skip over 
						//that item but continue on with the cursor till it runs out:
						List<TypedRow> typedRows = getNext(cursor);
						if (typedRows == null)
							break;
						for (TypedRow prow:typedRows) {
							SearchPosting.addSolrDataDoc(prow, table, tablesolrDocs);
						}
						if (tablesolrDocs.size() > REINDEX_BATCH_SIZE) {
							docsindexed+=solrDocs.size();
							//System.out.println("hit solr doc batch size in a searchable table, "+docsindexed+" docs so far, "+(System.currentTimeMillis()-startTime)+" millis elapsed "+(docsindexed/((System.currentTimeMillis()-startTime)/1000))+" docs per sec.");
							SearchPosting.saveSolr("reindex", tablesolrDocs, null);
							tablesolrDocs = new ArrayList<SolrInputDocument>();
						}
					}
					SearchPosting.saveSolr("reindex", tablesolrDocs, null);
					docsindexed+=solrDocs.size();
				}
				catch (Exception e) {
					log.warn("got an exception while indexing a searchable table with the query (probably a corrupt index in playorm):");
					log.warn(sql);
					log.warn("underlying exception"+ e);
					e.printStackTrace();
				}
			}
			if (solrDocs.size() > REINDEX_BATCH_SIZE) {
				docsindexed+=solrDocs.size();
				long elapsed = System.currentTimeMillis()-startTime;
				if (elapsed < 1) elapsed = 1;
				//System.out.println("hit solr doc batch size in metadata, "+docsindexed+" docs so far, "+(elapsed)+" millis elapsed "+
				//		((docsindexed/elapsed)*1000)+" docs per sec.");
				SearchPosting.saveSolr("reindex", solrDocs, "databusmeta");
				solrDocs = new ArrayList<SolrInputDocument>();
			}
		}
		if (solrDocs.size() > 0) {
			docsindexed+=solrDocs.size();
			long elapsed = System.currentTimeMillis()-startTime;
			if (elapsed < 1) elapsed = 1;
			//System.out.println("hit solr doc batch size during finalization, "+docsindexed+" docs so far, "+(elapsed)+" millis elapsed "+
			//		((docsindexed/elapsed)*1000)+" docs per sec.");
			SearchPosting.saveSolr("reindex", solrDocs, "databusmeta");
			solrDocs = new ArrayList<SolrInputDocument>();
		}
	}
	
	private static List<TypedRow> getNext(Iterator<List<TypedRow>> cursor) {
		try {
			if (cursor.hasNext())
				return cursor.next();
			return null;
		}
		catch (RowNotFoundException e) {
			return getNext(cursor);
		}
	}


	public static void indexSchemas() throws IOException, SAXException, ParserConfigurationException, SolrServerException {
		List<SecureSchema> schemas = SecureSchema.findAll(NoSql.em());
		for (SecureSchema schema:schemas) {
			SearchUtils.indexSchema(schema);
		}		
	}


	private static void deleteExistingCores() throws SolrServerException, IOException, ParserConfigurationException, SAXException {
		CoreAdminRequest request = new CoreAdminRequest();
		request.setAction(CoreAdminAction.STATUS);
		try {
			CoreAdminResponse cores = request.process(Search.getSolrServer());
			// List of the cores
			List<String> coreList = new ArrayList<String>();
			for (int i = 0; i < cores.getCoreStatus().size(); i++) {
			    coreList.add(cores.getCoreStatus().getName(i));
			}
			
			for (String coreName:coreList) {
				CoreAdminRequest.Unload unload = new CoreAdminRequest.Unload(true); 
				unload.setCoreName(coreName);
				unload.process(Search.getSolrServer()); 
			}
			knownCores.clear();
		}
		catch (Exception e) {
			//in this case, there are no cores to delete...
		}

		
	}


}
