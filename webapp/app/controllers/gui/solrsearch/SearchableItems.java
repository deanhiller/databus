package controllers.gui.solrsearch;

import java.util.Iterator;
import java.util.List;

public class SearchableItems {
	private List <SearchItem> searchableItems;

	public List<SearchItem> getSearchableItems() {
		return searchableItems;
	}

	public void setSearchableItems(List<SearchItem> searchableItems) {
		this.searchableItems = searchableItems;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append("{");
		result.append("\"searchableItems\" : [");
		
		Iterator<SearchItem> searchItr = this.searchableItems.iterator();
		while(searchItr.hasNext()) {
			SearchItem item = searchItr.next();
			
			result.append(item.toString());
			
			if(searchItr.hasNext()) {
				result.append(",");
			}
		}
		
		result.append("]}");
		
		return result.toString();
	}
}
