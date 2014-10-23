package controllers;

import gov.nrel.util.SearchUtils;
import gov.nrel.util.Utility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import models.EntityUser;
import models.SecureResource;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecureTable;
import models.SecurityGroup;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.orm.api.z8spi.meta.TypedColumn;
import com.alvazan.orm.api.z8spi.meta.TypedRow;
import com.alvazan.play.NoSql;
import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
import play.mvc.Util;
import play.mvc.results.Unauthorized;

public class Search extends Controller {
	private static final Logger log = LoggerFactory.getLogger(Search.class);
	private static final int PER_PAGE = 10;
	
	private static SolrServer solrServer;
	public static Map<String, SolrServer> cores = new HashMap<String, SolrServer>();
	
	public static void results() throws SolrServerException, IOException, ParserConfigurationException, SAXException {
		String queryString = params.get("q");
		String[] filters = params.getAll("f");
		int currentPage = 1;
		if(params.get("page") != null) {
			currentPage = Integer.parseInt(params.get("page"));
		}
		
		SolrQuery query = new SolrQuery(queryString);
		// Use the Extended DisMax query parser for more natural, user inputted queries.
		query.setParam("defType", "edismax");
		
		// Only return table results.
		query.addFilterQuery("type:*deviceMeta OR type:*streamMeta");
		
		if(filters != null) {
			// Group all the individual filters into arrays based on the field
			// name.
			Map<String, ArrayList<String>> filterFields = new HashMap<String, ArrayList<String>>();
			for(String filter : filters) {
				String[] filterParts = filter.split(":");
				String filterField = filterParts[0];
				String filterValue = filterParts[1];
				
				ArrayList<String> filterValues = filterFields.get(filterField);
				if(filterValues == null) {
					filterValues = new ArrayList<String>();
					filterFields.put(filterField, filterValues);
				}
				
				filterValues.add(filterValue);
			}
			
			// Add all the filters.
			for(Map.Entry<String, ArrayList<String>> entry : filterFields.entrySet()) {
				String filterField = entry.getKey();
				String filterTag = filterField.replace("_sms", "Filter");
				ArrayList<String> filterValues = entry.getValue();
				
				// Tag each filter so it can be excluded in the facet logic.
				// Perform the filter based on OR logic for the given field.
				query.addFilterQuery("{!tag=" + filterTag + "}" + filterField + ":(" + StringUtils.join(filterValues, " OR ") + ")");
			}
		}
		
		// Pagination
		query.setRows(PER_PAGE);
		query.setStart((currentPage - 1) * PER_PAGE);
		
		// Return everything if no search parameter is passed in.
		query.setParam("q.alt", "*:*");
		
		// Specify the fields to search in.
		query.setParam("qf", "id type description_texts endUse_texts building_texts protocol_texts site_texts owner_texts");
		
		// Setup HTML highlighting
		query.setHighlight(true);
		query.addHighlightField("id");
		query.addHighlightField("description_texts");
		query.setParam("hl.simple.pre", "<strong>");
		query.setParam("hl.simple.post", "</strong>");
		query.setParam("f.type.hl.snippets", "2");
		
		// Define all the fields to facet on.
		query.addFacetField("{!ex=endUseFilter}endUse_sms");
		query.addFacetField("{!ex=buildingFilter}building_sms");
		query.addFacetField("{!ex=protocolFilter}protocol_sms");
		query.addFacetField("{!ex=siteFilter}site_sms");
		query.addFacetField("{!ex=ownerFilter}owner_sms");
		
		QueryResponse response = getSolrServer().query(query);
		SolrDocumentList results = response.getResults();
				
		ArrayList<Integer> pages = new ArrayList<Integer>();
		int totalPages = (int) Math.ceil(results.getNumFound() / (float) PER_PAGE);
		for(int i = 1; i <= totalPages; i++) {
			pages.add(i);
		}
		
		render(response, results, currentPage, pages);
	}
	
	public static void result(String id) throws SolrServerException, IOException, ParserConfigurationException, SAXException {
		SolrQuery deviceQuery = new SolrQuery("type:*deviceMeta AND id:" + id);
		QueryResponse deviceResponse = getSolrServer().query(deviceQuery);
		SolrDocument device = deviceResponse.getResults().get(0);
		
		SolrQuery streamQuery = new SolrQuery("type:*streamMeta AND device_sms:" + id);
		QueryResponse streamResponse = getSolrServer().query(streamQuery);
		SolrDocumentList streams = streamResponse.getResults();
		
		render(device, streams);
	}
	
	public static SolrServer getSolrServer() throws IOException, ParserConfigurationException, SAXException {
		if(solrServer == null) {
			String prop = Play.configuration.getProperty("solr.mode");
			if (!SearchUtils.solrIsActivated()) {
				return null;
			}
			else if("embedded".equals(prop)) {
				String solrHome = "../solr/solr";
				System.setProperty("solr.solr.home", solrHome);
				CoreContainer.Initializer initializer = new CoreContainer.Initializer();
				CoreContainer coreContainer = initializer.initialize();
				solrServer = new EmbeddedSolrServer(coreContainer, "");
			} else {
				String url = Play.configuration.getProperty("solr.url");
				if(url == null)
					throw new IllegalArgumentException("must supply solr.url property");
				solrServer = new HttpSolrServer(url);
			}
		}
		
		return solrServer;
	}
	
	public static SolrServer getSolrCore(String coreName) throws IOException, ParserConfigurationException, SAXException {
		if(cores.get(coreName) == null) {
			String prop = Play.configuration.getProperty("solr.mode");
			if ("off".equals(prop)) {
				return null;
			}
			else if("embedded".equals(prop)) {
				//TODO:JSC - embedded and multicore... yes or no?
				String solrHome = "../solr/solr/"+coreName;
				System.setProperty("solr.solr.home", solrHome);
				CoreContainer.Initializer initializer = new CoreContainer.Initializer();
				CoreContainer coreContainer = initializer.initialize();
				cores.put(coreName, new EmbeddedSolrServer(coreContainer, ""));
			} else {
				String url = Play.configuration.getProperty("solr.url");
				url = StringUtils.removeEnd(url, "/");
				if(url == null)
					throw new IllegalArgumentException("must supply solr.url property");
				cores.put(coreName,new HttpSolrServer(url+"/"+coreName));
			}
		}
		
		return cores.get(coreName);
	}

	public static void searchTable(String table) {
		Map<String, String[]> all = Request.current().params.all();
		
		SecureTable secureTable = null;
		if (!StringUtils.equals("databusmeta", table))
			secureTable = SecurityUtil.checkSingleTable(table);
		
		Response.current().print(getSearchTable(table, secureTable, all));
	}
	
	public static String secureSearchTable(String table, Map<String, String[]> params,  EntityUser user, Request request) {
		SecureTable secureTable = null;
		if (!StringUtils.equals("databusmeta", table))
			secureTable = SecurityUtil.checkSingleTable(table, user, request);
		
		return(getSearchTable(table, secureTable, params));
	}
	
	public static String getSearchTable(String table, SecureTable secureTable, Map<String, String[]> all) {
		String result = "";
		
		String url = Play.configuration.getProperty("solr.url");
		url = StringUtils.removeEnd(url, "/");
		
		String params= buildParams(all, secureTable);			
		String requestHandler = Play.configuration.getProperty("solr.queryRequestHandler");
		requestHandler = StringUtils.removeEnd(requestHandler, "/");
		requestHandler = StringUtils.removeStart(requestHandler, "/");
		
		url += "/"+table+"/"+requestHandler+params;
		//System.out.println("Getting url "+url);
		RequestBuilder b = new RequestBuilder("GET").setUrl(url);
		AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		try {
			com.ning.http.client.Response response = asyncHttpClient.executeRequest(b.build(), new AsyncCompletionHandlerSearch()).get();
			result = response.getResponseBody();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
		catch (IOException e) {
            throw new RuntimeException(e);
        }
		
		return result;
	}
	
	private static String buildParams(Map<String, String[]> all, SecureTable secureTable) {
		String params="?";
		
		//setup params to pass to solr:
		Map<String, String[]> solrparams = new HashMap<String, String[]>();
		solrparams.putAll(all);
		
		setupInferredFacets(secureTable, solrparams);
		if (!solrparams.containsKey("df")) {
			solrparams.put("df",  new String[]{Play.configuration.getProperty("solr.defaultSearchField")});
		}
		
		for (String key:solrparams.keySet()) {
			for (String val:solrparams.get(key)) {
				if (!StringUtils.equals("body", key)) {
					try {
						params+=key+"="+ java.net.URLEncoder.encode(val, "UTF-8") +"&";
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		params = StringUtils.removeEnd(params, "&");
		return params;

	}

	private static void setupInferredFacets(SecureTable secureTable, Map<String, String[]> solrparams) {
		
		if (!solrparams.containsKey("facet.field")) {
			if (secureTable != null) {
				String facetFields = secureTable.getFacetFields();
				if (StringUtils.isNotBlank(facetFields)) {
					ArrayList<String> facets = new ArrayList<String>();
					for (String s:StringUtils.split(facetFields, ",")) {
						facets.add(s+"_sms");
					}
					solrparams.put("facet.field", facets.toArray(new String[]{}));
				}
			}
			//databusMeta:
			else {
				solrparams.put("facet.field", new String[]{"database_texts", "type", "creator_texts", "tag_sms"});
			}
		}
	}
	
	private static boolean userHasReadForSchema(SecureSchema schema) {
		return true;
	}

	private static Map<String, List<String>> getTablesAvailableToUser(EntityUser user) {
		Map<String, List<String>> combinedResults = new HashMap<String, List<String>>();
		Cursor<KeyValue<SecureTable>> searchableTables = SecureTable.findAllSearchable(NoSql.em());
		while (searchableTables.next()) {
			KeyValue<SecureTable> kv = searchableTables.getCurrent();
			SecureTable t = kv.getValue();
			if (t != null) {
				SecureSchema s = t.getSchema();
				if (userHasReadForSchema(s)) {
					List<String> tableNamesForSchema = combinedResults.get(s.getSchemaName());
					if (tableNamesForSchema==null) {
						tableNamesForSchema = new ArrayList<String>();
						combinedResults.put(s.getSchemaName(), tableNamesForSchema);
					}
					tableNamesForSchema.add(t.getName());
				}
			}
		}
		
		/*  I think starting at the table side will be faster than this:
		Map<String, SecureResourceGroupXref> schemas = SecurityUtil.groupSecurityCheck();
		for (Entry<String, SecureResourceGroupXref> ref:schemas.entrySet()) {
			SecureResource target = ref.getValue().getResource();
			if (!(target instanceof SecureTable)) {
				SecureSchema s = (SecureSchema)target;
				CursorToMany<SecureTable> tablesCursor = s.getTablesCursor();
				ArrayList<String> result = new ArrayList<String>();
				while (tablesCursor.next()) {
					SecureTable t = tablesCursor.getCurrent();
					if (t.isSearchable() && !result.contains(t.getName())) {
						result.add(t.getName());
					}
				}
				combinedResults.put(s.getSchemaName(), result);
			}
			
		}
		*/
		return combinedResults;
	}

	public static void searchableItems() {
		renderJSON(getSearchableItems());				
	}
	
	public static String getSearchableItems() {
		EntityUser user = Utility.getCurrentUser(session);
		
		if (user == null) {
			String username = Request.current().user;
			if (!StringUtils.isBlank(username))
				user = EntityUser.findByName(NoSql.em(), username);
		}
		
		String response;
		if(user == null) {
			response = "{\"searchableItems\":[";
			response += "{\"db\": \"databusmeta\",\"id\": \"databusmeta\",\"type\":\"meta\"}]}";
		}
		else {
			Map<String, List<String>> tables = getTablesAvailableToUser(user);
	
			response = "{\"searchableItems\":[";
			for (Entry<String, List<String>> entry:tables.entrySet()) {
				for (String t:entry.getValue())
					response += "{\"db\": \"" + entry.getKey() + "\",\"id\": \""+t+"\",\"type\":\"table\"},";
			}

			response += "{\"db\": \"databusmeta\",\"id\": \"databusmeta\",\"type\":\"meta\"}]}";
		}
		
		return response;
	}
	
	
	private static class AsyncCompletionHandlerSearch extends AsyncCompletionHandlerBase
    {
		
        @Override
        public com.ning.http.client.Response onCompleted(final com.ning.http.client.Response response) throws Exception {
        	//System.out.println("got response from solr:");
        	String responseBody = response.getResponseBody();
        	//System.out.println(responseBody);
            return super.onCompleted(response);
        }

        public void onThrowable(Throwable t) {
            t.printStackTrace();
        }
    }
	
	
}
