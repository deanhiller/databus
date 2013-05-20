package controllers.modules2;

import java.util.LinkedList;
import java.util.List;

import controllers.modules2.framework.TSRelational;

public class RelationalContext {
	private int maxPrevValues = 10;
	private LinkedList<TSRelational> previous = new LinkedList<TSRelational>();
	private long rowNum = 0;

	public void addPrevious(TSRelational lastValue) {
		previous.add(0, lastValue);
		if (previous.size() > maxPrevValues)
			previous.removeLast();
	}
	
	public TSRelational previousRow(int index) {
		return previous.get(index);
	}
	
	public Object previousValue(int index, String columnName) {
		if ((previous.size()-1)<index)
			return null;
		return previous.get(index).get(columnName);
	}

	public int getMaxPrevValues() {
		return maxPrevValues;
	}

	public void setMaxPrevValues(int maxPrevValues) {
		this.maxPrevValues = maxPrevValues;
	}

	public long getRowNum() {
		return rowNum;
	}

	public void setRowNum(long currentRow) {
		this.rowNum = currentRow;
	}
	
	public void incRowNum() {
		rowNum++;
	}
	
	
	
	
	

}
