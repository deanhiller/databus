package robot;

import gov.nrel.util.StartupBean;
import gov.nrel.util.StartupDetailed;
import gov.nrel.util.StartupGroups;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.GroupKey;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.test.FunctionalTest;


public class RunSolrPost {

	private final static Logger log = LoggerFactory.getLogger(RegisterPostAndGet.class);
	
	private int port = Utility.retrievePlayServerPort();
	private static final boolean SEARCHABLE = false;
	
	
	@Test
	public void registerPostAndGet() throws JsonGenerationException, JsonMappingException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		long r = System.currentTimeMillis();
		String tableName = "solrsPinkBlob"+r+"deviceMeta";
		String tableName2 = "solrsPinkBlob"+r+"streamMeta";
		registerNewStream(httpclient, tableName, null, null, null);

		RegisterResponseMessage resp2 = registerNewStream(httpclient, tableName2, tableName, "owner", "id");
		
		postNewDataPoint(httpclient, "bacNet1", "Lighting", "STM", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202 owner1", tableName);
	    postNewDataPoint(httpclient, "bacNet2", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL owner2", tableName);
	    
	    postNewDataPoint(httpclient, "bacNet10", "Lighting", "NTM", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202", tableName);
	    postNewDataPoint(httpclient, "bacNet20", "Lighting", "NTM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL", tableName);
	    
	    postNewDataPoint(httpclient, "bacNet11", "Lighting", "DenverWest", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202", tableName);
	    postNewDataPoint(httpclient, "bacNet21", "Lighting", "DenverWest", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL", tableName);
	    
	    postNewDataPoint(httpclient, "bacNet12", "Elevator", "STM", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202", tableName);
	    postNewDataPoint(httpclient, "bacNet22", "Elevator", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL", tableName);
	    
	    postNewDataPoint(httpclient, "bacNet13", "Elevator", "NTM", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202", tableName);
	    postNewDataPoint(httpclient, "bacNet23", "Elevator", "NTM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL", tableName);
	    
	    postNewDataPoint(httpclient, "bacNet14", "Elevator", "DenverWest", "RSF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "RSF 2ND FLR VAV-E1-202", tableName);
	    postNewDataPoint(httpclient, "bacNet24", "Elevator", "DenverWest", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "NREL", "BACNet", "SERF PCW_CONTROL", tableName);
	    
	    
	    
	    
	    postNewDataPoint(httpclient, "stream111", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 1", tableName2);
	    postNewDataPoint(httpclient, "stream112", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c1])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 1", tableName2);
	    postNewDataPoint(httpclient, "stream113", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c2])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 1", tableName2);
	    postNewDataPoint(httpclient, "stream114", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c3])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 1", tableName2);
	    
	    postNewDataPoint(httpclient, "stream211", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c0])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 2", tableName2);
	    postNewDataPoint(httpclient, "stream212", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c1])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 2", tableName2);
	    postNewDataPoint(httpclient, "stream213", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c2])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 2", tableName2);
	    postNewDataPoint(httpclient, "stream214", "Lighting", "STM", "SERF", "Address(networkNumber=50100, macAddress=[c0,a8,2,fd,ba,c3])", "bacNet1", "BACNet", "SERF PCW_CONTROL child of 2", tableName2);
		
	    
		//getData(tableName, getKey);
	}

	private void postNewDataPoint(DefaultHttpClient httpclient,
			String id, String endUse, String site, String bldg,
			String address, String owner, String protocl, String desc,
			String tableName) throws JsonGenerationException, JsonMappingException, IOException {
		Map result = createPoint(id, endUse, site, bldg, address, owner, protocl, desc, tableName);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(result);
		Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/postdata", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
	}

	private Map createPoint(String id, String endUse, String site,
			String bldg, String address, String owner, String protocl,
			String desc, Object tableName) {
		Map result = new HashMap();
		result.put("_tableName", tableName);
		result.put("id", id);
		result.put("endUse", endUse);
		result.put("site", site);
		result.put("building", bldg);
		result.put("address", address);
		result.put("owner", owner);
		result.put("protocol", protocl);
		result.put("description", desc);
		return result;
	}
	
	private RegisterResponseMessage registerNewStream(DefaultHttpClient httpclient, String tableName, String parentTableName, String childFKField, String parentFKField) 
			throws IOException, JsonGenerationException,
			JsonMappingException, UnsupportedEncodingException,
			ClientProtocolException, JsonParseException {

		String json = 
				"{  \"datasetType\": \"RELATIONAL_TABLE\",  \"modelName\": \""+tableName+"\",";
						if (!StringUtils.isBlank(parentTableName)) {
							json+="\"searchableParent\": [ {\"parentTable\":\""+parentTableName+"\", \"childFKField\":\""+childFKField+"\", \"parentFKField\":\""+parentFKField+"\"}],";
						}
							
						json+="\"isSearchable\": \""+SEARCHABLE+"\",	\"schema\": \"supa\","+
						"\"columns\":["+
							"{ \"name\": \"id\",\"dataType\": \"String\",\"isIndex\": true,\"isPrimaryKey\": true, \"isFacet\": false},"+
							"{\"name\": \"endUse\",	\"dataType\": \"String\",\"isIndex\": false,\"isPrimaryKey\": false,\"isFacet\": true},"+
							"{\"name\": \"site\",\"dataType\": \"String\",\"isIndex\": false,\"isPrimaryKey\": false,\"isFacet\": true},"+
							"{ \"name\": \"building\",\"dataType\": \"String\",\"isIndex\": false,\"isPrimaryKey\": false,\"isFacet\": true},"+
							"{\"name\": \"owner\",\"dataType\": \"String\",\"isIndex\": false, \"isPrimaryKey\": false},"+
							"{  \"name\": \"protocol\",  \"dataType\": \"String\",  \"isIndex\": false,  \"isPrimaryKey\": false,\"isFacet\": true},"+
							"{  \"name\": \"address\",  \"dataType\": \"String\",  \"isIndex\": false,  \"isPrimaryKey\": false},"+
							"{  \"name\": \"description\",  \"dataType\": \"String\",  \"isIndex\": false,  \"isPrimaryKey\": false} "+
						"]}";
		
		String theString = Utility.sendPostRequest(httpclient, "http://localhost:" + port + "/register", json, StartupGroups.ROBOT_USER, StartupGroups.ROBOT_KEY);
		
		ObjectMapper mapper = new ObjectMapper();
		RegisterResponseMessage resp = mapper.readValue(theString, RegisterResponseMessage.class);
		return resp;
	}

}
