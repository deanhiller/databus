package controllers.gui.solrsearch;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;


public class SolrSearchResult {
	int count;
	String result;
	SearchItem searchItem;
	
	public SolrSearchResult(SearchItem searchItem) {
		this.count = 0;
		this.result = "";
		this.searchItem = searchItem;
	}
	
	public SolrSearchResult(String result, SearchItem searchItem) {
		this.result = result;
		this.searchItem = searchItem;
		
		getCountFromResult();
	}
	
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
		
		getCountFromResult();
	}

	public SearchItem getSearchItem() {
		return searchItem;
	}
	
	public void setSearchItem(SearchItem searchItem) {
		this.searchItem = searchItem;
	}
	
	public int getCount() {
		return this.count;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append("\nResult for:\n");
		result.append("\tDB [" + this.searchItem.getDb() + "] : TABLE [" + this.searchItem.getId() + "]\n");
		result.append("\tRESULT COUNT [" + this.count + "]\n");
		result.append("<\n" + this.result + "\n>\n");
		
		return result.toString();
	}
	
	private void getCountFromResult() {
		int theCount = 0;
		
		if((this.result == null) || (this.result.equals(""))) {
			this.count = theCount;
			return;
		}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode;
		JsonNode responseNode;
		try {
			rootNode = mapper.readTree(this.result);
			responseNode = rootNode.path("response");
		} catch (Exception e) {
			System.out.println("SolrSearchResult.getCountFromResult() JSON EXCEPTION: [" + e.getMessage() + "]");
			this.count = theCount;
			return;
		} 
		
		if((rootNode == null) || (rootNode.isNull())) {
			this.count = theCount;
			return;
		}
		
		if((responseNode == null) || (responseNode.isNull())) {
			this.count = theCount;
			return;
		}
		
		try {
			this.count = responseNode.path("numFound").getIntValue();
		} catch (Exception e) {
			System.out.println("SolrSearchResult.getCountFromResult() JsonNode.getIntValue() ERROR: [" + e.getMessage() + "]");
			this.count = theCount;
			return;
		} 
	}
}
