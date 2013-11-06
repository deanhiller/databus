package controllers.modules2;

import java.util.LinkedList;
import java.util.List;

import controllers.modules2.framework.TSRelational;

public class RelationalContext {
	private int maxPrevValues = 10;
	private LinkedList<TSRelational> previous = new LinkedList<TSRelational>();
	private LinkedList<TSRelational> previousReturnedRow = new LinkedList<TSRelational>();
	private long rowNum = 0;

	public void addPrevious(TSRelational lastValue) {
		previous.add(0, lastValue);
		if (previous.size() > maxPrevValues)
			previous.removeLast();
	}
	
	public void addPreviousReturned(TSRelational lastValue) {
		previousReturnedRow.add(0, lastValue);
		if (previousReturnedRow.size() > maxPrevValues)
			previousReturnedRow.removeLast();
	}
	
	/**
	 * This is the previous SOURCE row, not necessarily the last row returned,
	 * since it's possible that some synthetic row was returned by a script. 
	 * 
	 * For the last row RETURNED to the parent, use previousReturnedRow(int index)
	 * 
	 * @param index
	 * @return
	 */
	public TSRelational previousRow(int index) {
		return previous.get(index);
	}
	
	public Object previousValue(int index, String columnName) {
		if ((previous.size()-1)<index)
			return null;
		return previous.get(index).get(columnName);
	}
	
	/**
	 * This is the previous RETURNED row, not the last SOURCE row taken from the child processor.
	 * If a script returned a synthetic row that's different from the original row from the child 
	 * processor.
	 * 
	 * For the last row from the SOURCE, use previousRow(int index)
	 * 
	 * @param index
	 * @return
	 */
	public TSRelational previousReturnedRow(int index) {
		return previousReturnedRow.get(index);
	}
	
	public Object previousReturnedValue(int index, String columnName) {
		if ((previousReturnedRow.size()-1)<index)
			return null;
		return previousReturnedRow.get(index).get(columnName);
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
