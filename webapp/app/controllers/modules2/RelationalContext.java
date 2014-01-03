package controllers.modules2;

import java.util.LinkedList;
import java.util.List;

import controllers.modules2.framework.TSRelational;

public class RelationalContext {
	private int maxPrevValues = 10;
	private LinkedList<TSRelational> previous = new LinkedList<TSRelational>();
	private LinkedList<TSRelational> previousReturnedRow = new LinkedList<TSRelational>();
	private long rowNum = 0;
	private boolean complete = true;
	private boolean discardRow = false;

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
	
	public TSRelational insertEmptySyntheticRow() {
		setComplete(false);
		return new TSRelational();
	}
	
	public TSRelational insertClonedSourceSyntheticRow() {
		setComplete(false);
		return new TSRelational(previousRow(0));
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
	
	/**
	 * if IsComplete it means that the source row has been 'consumed' and the next time 'read' is called 
	 * a new source row will be provided to the script.  if isComplete is false, it means that the script is 
	 * not yet 'done' with the current source row, so the next time 'read' is called the SAME source row will 
	 * still be provided.  This allows for situations like an interpolation where the original source row is 
	 * potentially used for multiple iterations through the script as synthetic rows are inserted.
	 * 
	 * This value is set to true by default and is only set to false if explicitly set by the script or if 
	 * insertEmptySyntheticRow() or insertClonedSourceSyntheticRow() are called.
	 * 
	 * @return
	 */
	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * if discardRow is true no row is returned.  This is useful for situations like a 'cleaner' where for 
	 * some reason the original source row is never returned to the parent processor.  This is only set 
	 * to true if explicitly set by the script.
	 * @return
	 */
	public boolean isDiscardRow() {
		return discardRow;
	}

	public void setDiscardRow(boolean discardRow) {
		this.discardRow = discardRow;
	}
	
	

}
