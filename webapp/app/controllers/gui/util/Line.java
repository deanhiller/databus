package controllers.gui.util;

import controllers.gui.SocketState;

public class Line {

	private int lineNumber;
	private String[] columns;
	private int length;
	private String row;
	private SocketState state;

	public Line(int lineNum, int len, String row, SocketState state) {
		this.lineNumber = lineNum;
		this.length = len;
		this.row = row;
		this.state = state;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String[] getColumns() {
		if(columns == null)
			init();
		return columns;
	}

	public int getLength() {
		return length;
	}

	public void setText(String row) {
		this.row = row;
	}
	
	private void init() {
		columns = new String[state.getNumHeaderCols()];
		int pos = 0, end;
		int colIndex = 0;
        while ((end = row.indexOf(',', pos)) >= 0) {
        	if (colIndex > state.getNumHeaderCols()) {
        		String msg = "line number "+lineNumber+" has wrong number of arguments. num="+columns.length+" expected was="+state.getNumHeaderCols()+" based on the headers";
				if(columns.length == 1 && "".equals(columns[0].trim()))
					msg = "line number="+lineNumber+" contains no columns(no commas) so we are skipping the line";
				state.reportErrorAndClose(msg);
				return;
        	}
        	columns[colIndex]=row.substring(pos, end);
            pos = end + 1;
            colIndex++;
        }
        if (pos < row.length())
        	columns[colIndex]=row.substring(pos, row.length());
	}
}
