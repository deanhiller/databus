package controllers.modules2.framework;

import java.util.ArrayList;
import java.util.List;

public class Path {

	private List<String> params = new ArrayList<String>();
	private String previousPath;
	private String leftOverPath;
	private Long start;
	private Long end;
	private boolean isReversed;
	
	public Path(List<String> params, String previousPath, String leftOverPath, Long start, Long end, boolean isReversed) {
		super();
		this.previousPath = previousPath;
		this.params = params;
		this.leftOverPath = leftOverPath;
		this.start = start;
		this.end = end;
		this.isReversed = isReversed;
	}
	public List<String> getParams() {
		return params;
	}
	public String getLeftOverPath() {
		return leftOverPath;
	}
	
	public Long getOriginalStart() {
		return start;
	}
	public Long getOriginalEnd() {
		return end;
	}
	public Long getStart() {
		if(isReversed) {
			if(end == null)
				return null;
			return -end;
		}
		return start;
	}
	public Long getEnd() {
		if(isReversed) {
			if(start == null)
				return null;
			return -start;
		}
		return end;
	}
	
	public String getPreviousPath() {
		return previousPath;
	}
	public boolean isReversed() {
		return isReversed;
	}
}