package controllers.gui.util;

public class Line {

	private int lineNumber;
	private String[] columns;

	public Line(int i, String[] cols) {
		this.lineNumber = i;
		this.columns = cols;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String[] getColumns() {
		return columns;
	}
	
}
