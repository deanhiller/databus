package controllers.modules;

import gov.nrel.util.TimeValue;

import java.util.ArrayList;
import java.util.List;

import controllers.modules.util.JsonRow;

public class SumCache {

	private boolean complete;
	private List<TimeValue> cachedRows = new ArrayList<TimeValue>();
	private int totalCount = 0;
	private String url;
	
	public SumCache(String url) {
		this.url = url;
	}
	
	public void setComplete(boolean b) {
		this.complete = b;
	}

	public boolean hasData() {
		if(cachedRows.size() > 0)
			return true;
		return false;
	}

	public void addChunk(JsonRow chunk) {
		cachedRows.addAll(chunk.getRows());
		totalCount += chunk.getRows().size();
	}

	public Integer getRowCount() {
		return cachedRows.size();
	}

	public TimeValue removeNextRow() {
		if(cachedRows.size() == 0)
			return null;
		return cachedRows.remove(0);
	}

	public String getUrl() {
		return url;
	}

	public int getTotalCount() {
		return totalCount;
	}
}
