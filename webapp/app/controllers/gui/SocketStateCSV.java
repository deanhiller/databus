package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.StringUtils;

import play.mvc.Http.Outbound;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import models.SecureTable;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;

import controllers.gui.util.ExecutorsSingleton;
import controllers.gui.util.Line;

public class SocketStateCSV extends SocketState {

	private int lineNumber = 0;
	protected List<String> headers;
	//protected List<DboColumnMeta> headers;
	
	public SocketStateCSV(NoSqlEntityManagerFactory factory, SecureTable sdiTable, ExecutorService executor, Outbound outbound) {
		this.factory = factory;
		this.sdiTable = sdiTable;
		this.executor = executor;
		this.outbound = outbound;
	}
	
	@Override
	protected void processFile(String s) throws IOException {
		buffer.append(s);
		
		int index = buffer.indexOf("\n");
		while(index >= 0) {
			String row = buffer.substring(0, index);
			buffer.replace(0, index+1, "");
			try {
				processItem(row, row.length()+1);
			} catch(RuntimeException e) {
				throw e;
				//throw new RuntimeException("exception processing row="+row+"   "+e.getMessage(), e);
			}
			index = buffer.indexOf("\n");
		}
	}
	
	protected void processItem(String row, int len) {
		lineNumber++; //increment the line number to this row's exact line number
		if(headers == null) {
			int pos = 0, end;
			ArrayList<String> cols = new ArrayList<String>();
            while ((end = row.indexOf(',', pos)) >= 0) {
                cols.add(row.substring(pos, end));
                pos = end + 1;
            }
            if (pos < row.length())
            	cols.add(row.substring(pos, row.length()));
            
			readHeaders(cols.toArray(new String[]{}));
			addTotalCharactersDone(len);
			return;
		}
		
		Line line = new Line(lineNumber, len, getColumns(headers, row));
		batch.add(line);
        
		//play.Logger.info("processing the string "+row+" count is "+count);
		if((lineNumber%BATCH_SIZE) == 0) {
			//play.Logger.info("calling into the executor with "+batch.size()+" items, row is "+row);
			executor.execute(new SaveBatch(tableMeta, batch, this, outbound));
			batch = new ArrayList<Line>();
		}

		if(log.isDebugEnabled() && lineNumber % BATCH_SIZE == 0)
			log.debug("Another "+BATCH_SIZE+" rows was passed to Runnables to write to database.  last line number="+lineNumber);
	}

	private LinkedHashMap<String, String> getColumns(List<String> headers, String row) {
		LinkedHashMap<String, String> columnsMap = new LinkedHashMap<String, String>();
		int pos = 0, end;
		int colIndex = 0;
		if(row.indexOf(',') < 0) {
			String msg = "line number="+lineNumber+" contains no columns(no commas) so we are skipping the line";
			reportErrorAndClose(msg);
			return columnsMap;
		}
        while ((end = row.indexOf(',', pos)) >= 0) {
        	if (colIndex > headers.size()) {
        		String msg = "line number "+lineNumber+" has wrong number of arguments. num="+(colIndex+1)+" expected was="+headers.size()+" based on the headers";
				reportErrorAndClose(msg);
				return columnsMap;
        	}
        	columnsMap.put(headers.get(colIndex), row.substring(pos,end));
            pos = end + 1;
            colIndex++;
        }
        if (pos < row.length()) {
        	if (headers.size() <= colIndex) {
        		String msg = "line number "+lineNumber+" has wrong number of arguments. num="+colIndex+" expected was="+headers.size()+" based on the headers";
    			reportErrorAndClose(msg);
    			return columnsMap;
        	}
        	columnsMap.put(headers.get(colIndex), row.substring(pos, row.length()));
        }

        return columnsMap;
	}

	protected void readHeaders(String[] cols) {
		SecureTable sTable = getSdiTable();
		tableMeta = sTable.getTableMeta();
		List<String> columns = new ArrayList<String>();
		for(String theCol : cols) {
			String col = theCol.trim();
			DboColumnIdMeta idMeta = tableMeta.getIdColumnMeta();
			DboColumnMeta colMeta = tableMeta.getColumnMeta(col);
			if(colMeta != null) {
				columns.add(colMeta.getColumnName());
			} else if(col.equals(idMeta.getColumnName())) {
				columns.add(idMeta.getColumnName());
			} else {
				throw new IllegalArgumentException("col="+col+" does not exist in this table");
			}
		}

		headers = columns;
	}

	public int getNumItemsSubmitted() {
		return lineNumber;
	}
}
