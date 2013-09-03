package sstable;

import java.util.ArrayList;
import java.util.List;

public class RowFrag {

	private List<String> columns = new ArrayList<String>();
	private String fileName;
	
	public RowFrag(String fileName) {
		this.fileName = fileName;
	}

	public void addColumn(String col) {
		this.columns.add(col);
		
	}

	@Override
	public String toString() {
		String s = "("+fileName+") ";
		for(String c : columns) {
			s+=c+",";
		}
		return s;
	}
	
}
