package controllers.modules.util;

import gov.nrel.util.TimeValue;

import java.util.List;
import java.util.Map;

public class JsonRow {

	private boolean isFirstChunk = false;
	private String url;
	private List<TimeValue> rows;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<TimeValue> getRows() {
		return rows;
	}

	public void setJsonSnippet(List<TimeValue> jsonSnippet) {
		this.rows = jsonSnippet;
	}

	public boolean isFirstChunk() {
		return isFirstChunk;
	}

	public void setFirstRow(boolean isFirstRow) {
		this.isFirstChunk = isFirstRow;
	}
}
