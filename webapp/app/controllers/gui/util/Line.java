package controllers.gui.util;

import java.util.LinkedHashMap;

import play.PlayPlugin;
import controllers.gui.SocketState;
import controllers.gui.SocketStateCSV;

public class Line extends PlayPlugin {

	private int lineNumber;
	private LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>();
	private int length;

	public Line(int lineNum, int len, LinkedHashMap<String, String> columns) {
		this.lineNumber = lineNum;
		this.length = len;
		this.columns = columns;
	}

	public int getLineNumber() {
		return lineNumber;
	}
	
	public void setColumns(LinkedHashMap<String, String> cols) {
		this.columns = cols;
	}

	public LinkedHashMap<String, String> getColumns() {
		return columns;
	}

	public int getLength() {
		return length;
	}
	
	public void setLength(int newlen) {
		length = newlen;
	}
}
