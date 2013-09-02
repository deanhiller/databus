package sstable;

public class RowCounter {

	private int rowCount;
	private int nullCounter;
	private int dataPointCounter;
	
	public void increment() {
		rowCount++;
	}

	public void incrementNullFound() {
		nullCounter++;
	}
	public void incrementPoint() {
		dataPointCounter++;
	}

	public int getRowCount() {
		return rowCount;
	}

	public int getNullCounter() {
		return nullCounter;
	}

	public int getDataPointCounter() {
		return dataPointCounter;
	}
	

}
