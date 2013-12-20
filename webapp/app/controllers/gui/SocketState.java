package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Outbound;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import models.SecureTable;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.gui.util.ExecutorsSingleton;
import controllers.gui.util.Line;

public class SocketState {

	private static final Logger log = LoggerFactory.getLogger(SocketState.class);
	
	static final int BATCH_SIZE = 5000;
	static final int ERROR_LIMIT = 10;
	
	private NoSqlEntityManagerFactory factory;
	private SecureTable sdiTable;
	private ExecutorService executor;
	private Outbound outbound;

	private int fileSize;
	private int lineNumber = 0;
	private List<DboColumnMeta> headers;
	private StringBuffer buffer = new StringBuffer();

	private List<Line> batch = new ArrayList<Line>();
	private DboTableMeta tableMeta;

	private int doneCount = 1; //start at one to skip header(header is line 1 so no need to write it so it's done already)
	private int charactersDone;
	private List<String> errors = new ArrayList<String>();

	public SocketState(NoSqlEntityManagerFactory factory, SecureTable sdiTable, ExecutorService executor, Outbound outbound) {
		this.factory = factory;
		this.sdiTable = sdiTable;
		this.executor = executor;
		this.outbound = outbound;
	}
	
	void processFile(String s) throws IOException {
		buffer.append(s);
		
		int index = buffer.indexOf("\n");
		while(index >= 0) {
			String row = buffer.substring(0, index);
			buffer.replace(0, index+1, "");
			try {
				processRow(row, row.length()+1);
			} catch(RuntimeException e) {
				throw new RuntimeException("exception processing row="+row, e);
			}
			index = buffer.indexOf("\n");
		}
	}

	private void processRow(String row, int len) {
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
		
		Line line = new Line(lineNumber, len, row, this);
		batch.add(line);
        
		//play.Logger.info("processing the string "+row+" count is "+count);
		if((lineNumber%BATCH_SIZE) == 0) {
			//play.Logger.info("calling into the executor with "+batch.size()+" items, row is "+row);
			executor.execute(new SaveBatch(tableMeta, headers, batch, this, outbound));
			batch = new ArrayList<Line>();
		}

		if(log.isDebugEnabled() && lineNumber % 2000 == 0)
			log.debug("Another 2000 rows was passed to Runnables to write to database.  last line number="+lineNumber);
	}

	private void readHeaders(String[] cols) {
		SecureTable sTable = getSdiTable();
		tableMeta = sTable.getTableMeta();
		List<DboColumnMeta> columns = new ArrayList<DboColumnMeta>();
		for(String theCol : cols) {
			String col = theCol.trim();
			DboColumnIdMeta idMeta = tableMeta.getIdColumnMeta();
			DboColumnMeta colMeta = tableMeta.getColumnMeta(col);
			if(colMeta != null) {
				columns.add(colMeta);
			} else if(col.equals(idMeta.getColumnName())) {
				columns.add(idMeta);
			} else {
				throw new IllegalArgumentException("col="+col+" does not exist in this table");
			}
		}

		headers = columns;
	}

	public SecureTable getSdiTable() {
		return sdiTable;
	}

	public int getNumLinesSubmitted() {
		return lineNumber;
	}

	public int getDoneCount() {
		return doneCount;
	}
	public void setDoneCount(int count) {
		this.doneCount = count;
	}

	public NoSqlEntityManagerFactory getEntityManagerFactory() {
		return factory;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setSize(int size) {
		this.fileSize = size;
	}

	public void addTotalCharactersDone(int characterCount) {
		charactersDone += characterCount;
	}

	public int getCharactersDone() {
		return charactersDone;
	}

	public void endOfData() {
		//execute the last batch
		executor.execute(new SaveBatch(tableMeta, headers, batch, this, outbound));
	}

	public int getFileSize() {
		return fileSize;
	}

	public int getNumHeaderCols() {
		return headers.size();
	}

	public void reportErrorAndClose(String msg) {
		if(log.isWarnEnabled())
			log.warn(msg);
		if(errors.size() < ERROR_LIMIT)
			errors.add(msg);
		
		sendErrorThenClose();		
	}
	
	private void sendErrorThenClose() {
		String msg = errors.get(0);
		String newMsg = msg.replace("\"", "'");
		if(!outbound.isOpen())
			return;
		
		outbound.send("{ \"error\":\""+newMsg+"\" }");
		outbound.close();
	}
}
