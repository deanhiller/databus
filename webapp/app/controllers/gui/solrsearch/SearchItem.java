package controllers.gui.solrsearch;

public class SearchItem {
	private String db;
	private String id;
	private String type;
	
	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getSearchURI(String solrURL, String query) {
		StringBuffer searchURI = new StringBuffer(solrURL);
		
		if(this.getType().equals("table")) {
    			// This can be a detail search item
			// Detail URI looks like:
			//		http://localhost:9000/api/search/bacnetstreamMeta?facet=true&q=*&wt=json&rows=0
    			searchURI.append(this.getId() + "?facet=true&wt=json&rows=0&q=" + query);
    		} else {
    			// This is a databusmeta search item
    			// MetaSearch URI looks like:
    			//		http://localhost:9000/api/search/databusmeta?facet=true&q=*&wt=json&rows=0
    			searchURI.append("databusmeta?facet=true&wt=json&rows=0&q=" + query);
    		}			
		
		return searchURI.toString();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append("{");
		result.append("		\"db\" : ");
		result.append("\"" + this.db + "\",");
		result.append("		\"id\" : ");
		result.append("\"" + this.id + "\",");
		result.append("");
		result.append("		\"type\" : ");
		result.append("\"" + this.type + "\"");
		result.append("}");
		
		return result.toString();
	}
}
