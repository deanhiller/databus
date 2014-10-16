package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
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
import com.google.gson.stream.JsonReader;

import controllers.gui.util.ExecutorsSingleton;
import controllers.gui.util.Line;

public class SocketStateJSON extends SocketState {

	private int itemcount = 0;
	private int thisbatchcount = 0;
	private int thisbatch = 0;
	private String tableName = "";
	private int pendingCharCount = 0;
	private Line lastline = null;


	
	private int lineNumber = 0;
	public SocketStateJSON(NoSqlEntityManagerFactory factory, SecureTable sdiTable, ExecutorService executor, Outbound outbound) {
		this.factory = factory;
		this.sdiTable = sdiTable;
		if (sdiTable != null)
			this.tableName = sdiTable.getTableName();
		
		this.executor = executor;
		this.outbound = outbound;
	}
	
	
	@Override
	public void processFile(String s) throws IOException {
		SecureTable sTable = getSdiTable();
		tableMeta = sTable.getTableMeta();
		setDoneCount(0);
		buffer.append(s);
		
		thisbatchcount = 0;
		thisbatch++;
		
		MyStringReader reader = new MyStringReader(buffer.toString());
		JsonReader jsonReader = new JsonReader(reader);
		jsonReader.beginObject();
		boolean incompleteContent = false;
		if (jsonReader.hasNext()) {
			String name = jsonReader.nextName();
			if (name.equalsIgnoreCase("_dataset")) {
				jsonReader.beginArray();
			}
			else {
				jsonReader.close();
				throw new RuntimeException("bad content: "+buffer.toString());
				//jsonReader = new JsonReader(new StringReader(buffer.toString()));
			}
		}
			
		try {
			while (jsonReader.hasNext()) {
				
				jsonReader.beginObject();
				processItem(jsonReader);
				
				jsonReader.endObject();

			}
		}
		catch (Exception e) {
			//this case will occur either on the line
			//while (jsonReader.hasNext())
			//when the content happens to end with a missing comma as the last expected element, like this:
			//...
			//{"_tableName":"EVSE_09B", "time":1382854260000, "value":0.002967},
			//{"_tableName":"EVSE_09B", "time":1382854200000, "value":0.002950}
			//
			//OR almost all the time it will happen during chunking with incomplete content lines like this:
			//{"_tableName":"EVSE_09
			incompleteContent = true;
			//throw new RuntimeException(e);
		}
		String remaining = "";
		if (incompleteContent) {
			remaining = s.substring(s.lastIndexOf('{'), s.length());
			int charsDone = reader.next - remaining.length();
			log.info("charsDone is "+charsDone);
			pendingCharCount = charsDone>=0?charsDone:0;

			log.info("pendingCharCount is "+pendingCharCount);
			remaining = "{\"_dataset\":["+remaining;
			pendingCharCount=pendingCharCount-13;  //to account for the artificial chars added the line above.
		}
		else {
			pendingCharCount = reader.next;
			//this is the last chunk, we need to add its length to the char count
			if(lastline != null)
				lastline.setLength(pendingCharCount);
		}
		buffer = new StringBuffer();

		buffer.append(remaining);

		
//		int index = buffer.indexOf("\n");
//		while(index >= 0) {
//			String row = buffer.substring(0, index);
//			buffer.replace(0, index+1, "");
//			try {
//				processItem(row, row.length()+1);
//			} catch(RuntimeException e) {
//				throw new RuntimeException("exception processing row="+row, e);
//			}
//			index = buffer.indexOf("\n");
//		}
	}
	

	private void processItem(JsonReader jsonReader) throws IOException {
		itemcount++;
		thisbatchcount++;
		String tn = "";
		LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>();
		while (jsonReader.hasNext()) {
			String itemname = jsonReader.nextName();
			if ("_tableName".equals(itemname)) {
				tn = jsonReader.nextString();
				if (!this.tableName.equals(tn)) {
					reportErrorAndClose("Using this upload method all data specified in the json must be for the table identified in the upload url."+  
							"The table specified in the upload was '"+this.tableName+"' the table name included in the json is '"+tn+"' in item "+itemcount+" of the file.");
					jsonReader.close();
					return;
				}
			}
			else { 
				columns.put(itemname, jsonReader.nextString());
			}
		}	
		int size = 0;
		if (pendingCharCount != 0) {
			size = pendingCharCount;
			pendingCharCount = 0;
		}
		Line line = new Line(lineNumber, size, columns);
		lastline = line;
		batch.add(line);
        
		//play.Logger.info("processing the string "+row+" count is "+count);
		if((lineNumber%BATCH_SIZE) == 0) {
			log.info("calling into the executor with "+batch.size()+" items.  line number is "+lineNumber);
			executor.execute(new SaveBatch(tableMeta, batch, this, outbound));
			batch = new ArrayList<Line>();
			if(log.isDebugEnabled())
				log.debug("Another "+BATCH_SIZE+" rows was passed to Runnables to write to database.  last line number="+lineNumber);
		}

		lineNumber++;

		
		
	}

	public int getNumItemsSubmitted() {
		return lineNumber<=0?0:lineNumber-1;
	}
	
	private class MyStringReader extends Reader {

	    private String str;
	    private int length;
	    public int next = 0;
	    private int mark = 0;

	    /**
	     * Creates a new string reader.
	     *
	     * @param s  String providing the character stream.
	     */
	    public MyStringReader(String s) {
	        this.str = s;
	        this.length = s.length();
	    }

	    /** Check to make sure that the stream has not been closed */
	    private void ensureOpen() throws IOException {
	        if (str == null)
	            throw new IOException("Stream closed");
	    }

	    /**
	     * Reads a single character.
	     *
	     * @return     The character read, or -1 if the end of the stream has been
	     *             reached
	     *
	     * @exception  IOException  If an I/O error occurs
	     */
	    public int read() throws IOException {
	        synchronized (lock) {
	            ensureOpen();
	            if (next >= length)
	                return -1;
	            return str.charAt(next++);
	        }
	    }

	    /**
	     * Reads characters into a portion of an array.
	     *
	     * @param      cbuf  Destination buffer
	     * @param      off   Offset at which to start writing characters
	     * @param      len   Maximum number of characters to read
	     *
	     * @return     The number of characters read, or -1 if the end of the
	     *             stream has been reached
	     *
	     * @exception  IOException  If an I/O error occurs
	     */
	    public int read(char cbuf[], int off, int len) throws IOException {
	        synchronized (lock) {
	            ensureOpen();
	            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
	                ((off + len) > cbuf.length) || ((off + len) < 0)) {
	                throw new IndexOutOfBoundsException();
	            } else if (len == 0) {
	                return 0;
	            }
	            if (next >= length)
	                return -1;
	            int n = Math.min(length - next, len);
	            str.getChars(next, next + n, cbuf, off);
	            next += n;
	            return n;
	        }
	    }

	    /**
	     * Skips the specified number of characters in the stream. Returns
	     * the number of characters that were skipped.
	     *
	     * <p>The <code>ns</code> parameter may be negative, even though the
	     * <code>skip</code> method of the {@link Reader} superclass throws
	     * an exception in this case. Negative values of <code>ns</code> cause the
	     * stream to skip backwards. Negative return values indicate a skip
	     * backwards. It is not possible to skip backwards past the beginning of
	     * the string.
	     *
	     * <p>If the entire string has been read or skipped, then this method has
	     * no effect and always returns 0.
	     *
	     * @exception  IOException  If an I/O error occurs
	     */
	    public long skip(long ns) throws IOException {
	        synchronized (lock) {
	            ensureOpen();
	            if (next >= length)
	                return 0;
	            // Bound skip by beginning and end of the source
	            long n = Math.min(length - next, ns);
	            n = Math.max(-next, n);
	            next += n;
	            return n;
	        }
	    }

	    /**
	     * Tells whether this stream is ready to be read.
	     *
	     * @return True if the next read() is guaranteed not to block for input
	     *
	     * @exception  IOException  If the stream is closed
	     */
	    public boolean ready() throws IOException {
	        synchronized (lock) {
	        ensureOpen();
	        return true;
	        }
	    }

	    /**
	     * Tells whether this stream supports the mark() operation, which it does.
	     */
	    public boolean markSupported() {
	        return true;
	    }

	    /**
	     * Marks the present position in the stream.  Subsequent calls to reset()
	     * will reposition the stream to this point.
	     *
	     * @param  readAheadLimit  Limit on the number of characters that may be
	     *                         read while still preserving the mark.  Because
	     *                         the stream's input comes from a string, there
	     *                         is no actual limit, so this argument must not
	     *                         be negative, but is otherwise ignored.
	     *
	     * @exception  IllegalArgumentException  If readAheadLimit is < 0
	     * @exception  IOException  If an I/O error occurs
	     */
	    public void mark(int readAheadLimit) throws IOException {
	        if (readAheadLimit < 0){
	            throw new IllegalArgumentException("Read-ahead limit < 0");
	        }
	        synchronized (lock) {
	            ensureOpen();
	            mark = next;
	        }
	    }

	    /**
	     * Resets the stream to the most recent mark, or to the beginning of the
	     * string if it has never been marked.
	     *
	     * @exception  IOException  If an I/O error occurs
	     */
	    public void reset() throws IOException {
	        synchronized (lock) {
	            ensureOpen();
	            next = mark;
	        }
	    }

	    /**
	     * Closes the stream and releases any system resources associated with
	     * it. Once the stream has been closed, further read(),
	     * ready(), mark(), or reset() invocations will throw an IOException.
	     * Closing a previously closed stream has no effect.
	     */
	    public void close() {
	        str = null;
	    }
	}
}
