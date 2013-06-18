package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import models.PermissionType;
import models.SecureTable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.With;
import play.mvc.results.Unauthorized;
import play.server.ChunkListener;
import play.server.EntireBodyStream;
import play.server.HttpChunkStream;
import play.server.MultiPartStream;
import play.server.ReqState;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

import controllers.SecurityUtil;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class Tables extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Tables.class);
	static final int ERROR_LIMIT = 10;
	private static final int BATCH_SIZE = 5000;
	
	public static void uploadForm(String table) {
		SecureTable sdiTable = SecurityUtil.checkSingleTable(table);
		render(sdiTable, table);
	}

	public static void uploadSuccess(String table) {
		render();
	}

	public static void postData(String table) throws FileNotFoundException {
		if(request.isNew) {
			if (log.isDebugEnabled())
				log.debug("first call to controller method");
			PermissionType p = SecurityUtil.checkSingleTable2(table);
			if(PermissionType.READ_WRITE.isHigherRoleThan(p))
				throw new Unauthorized("You do not have a high enough permission to write to this table="+table);
			
			SecureTable sdiTable = SecureTable.findByName(NoSql.em(), table);
			DboTableMeta meta = sdiTable.getTableMeta();
			meta.initCaches();
			Collection<DboColumnMeta> columns = meta.getAllColumns();
			for(DboColumnMeta m : columns) {
				//force load from noSQL store right now...
				m.getColumnName();
			}

			NoSqlEntityManagerFactory factory = NoSql.getEntityManagerFactory();
			request.args.put("count", 0);
			request.args.put("factory", factory);
			request.args.put("table", sdiTable);
			request.args.put("errorsList", new ArrayList<String>());
			request.chunkListener = new OurListener();
			request.args.put("total", 0);
			
			if(request.isChunkedRequest)
				return;
			else {
				fireIntoListener(request);
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("final call to the controller method");

		//jsc comment this whole block for 100x
		synchronized(request) {
			while(!isComplete(request)) {
				try {
					if(log.isDebugEnabled())
						log.debug("csv upload - waiting to complete");
					request.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		Throwable t = (Throwable) request.args.get("error");
		if(t != null)
			throw new RuntimeException("failure saving all data", t);

		List<String> errors = (List<String>) request.args.get("errorsList");
		if(errors.size() > 0) {
			if(errors.size() >= ERROR_LIMIT)
				flash.error("You hit our error limit of="+ERROR_LIMIT+" so we don't report all the errors you had.  Those 10 errors are="+errors);
			else
				flash.error("Errors on some lines(numErrors="+errors.size()+") "+errors);
			flash.keep();
		}

		uploadSuccess(table);
	}

	private static void fireIntoListener(Request request) {
		HttpChunkStream stream = ReqState.createProcessor(request, response);
		stream.setChunkListener(request.chunkListener);
		ByteArrayOutputStream str = new ByteArrayOutputStream();
		try {
			IOUtils.copy(request.body, str);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte[] data = str.toByteArray();
		stream.addMoreData(data, true);
	}

	private static boolean isComplete(Request request) {
		Integer count = (Integer) request.args.get("count");
		Integer total = (Integer) request.args.get("total");
		if(log.isDebugEnabled())
			log.debug("csv upload - waiting for completion. total="+total+" count="+count);
		Throwable t = (Throwable) request.args.get("error");
		if(t != null)
			return true;
		else if (count == null || total == null)
			return false;
		else if(count >= total)
			return true;
		return false;
	}

	private static void reportError(Request request, String msg) {
		if(log.isWarnEnabled())
			log.warn(msg);
		Map<String, Object> map = request.args;
		Object obj = map.get("errorsList");
		List<String> errors = (List<String>) obj;
		if(errors.size() < ERROR_LIMIT) {
			errors.add(msg);
		}
	}
	
	private static class OurListener implements ChunkListener {
		private int noErrorCount = 0;
		private int count = 0;
		private boolean readingFile;
		private List<DboColumnMeta> headers;
		private int headersize = 2;
		//private StringBuilder buffer = new StringBuilder();
		private String buffer = "";

		private Line[] batch = new Line[BATCH_SIZE];
		private DboTableMeta tableMeta;
		
		//pre initialize entire batch (for performance)
		//this must be run after header information is loaded
		private void initializeBatch() {
			for (int i = 0; i < BATCH_SIZE; i++)
				batch[i]=new Line(-1, new String[headersize]);
		}

		@Override
		public void moreFormParamData(ByteArrayInputStream in, Request request,
				Response response) {
			try {
				
				if(readingFile) {
					if(request.args.get("error") != null)
						return; //we are done, there was an error
					
					//jsc comment this whole block for 10x
					processFile(in, request, response);
				}
			} catch(Exception e) {
				request.args.put("error", e);
				throw new RuntimeException(e);
			}
		}

		private void processFile(ByteArrayInputStream in, Request request,
				Response response) throws IOException {
			byte[] data = new byte[in.available()];
			in.read(data);
			String s = new String(data);
			buffer+=s;
			//jsc1
			//buffer.append(s);
			
			int index = buffer.indexOf("\n");
			while(index >= 0) {
				String row = buffer.substring(0, index);
				//jsc1
				//buffer.delete(0, index+1);
				buffer = buffer.substring(index+1);
				processRow(row, request, response);
				index = buffer.indexOf("\n");
			}
		}

		private void processRow(String row, Request request, Response response) {
			//String[] cols = row.split(",");
			
			if(headers == null) {
				int pos = 0, end;
				ArrayList<String> cols = new ArrayList<String>(headersize);
	            while ((end = row.indexOf(',', pos)) >= 0) {
	                cols.add(row.substring(pos, end));
	                pos = end + 1;
	            }
	            if (pos < row.length())
	            	cols.add(row.substring(pos, row.length()));
	            
				readHeaders(cols.toArray(new String[]{}), request);
				return;
			}
			
			int pos = 0, end;
			String[] cols = batch[count%BATCH_SIZE].getColumns();
			int colIndex = 0;
            while ((end = row.indexOf(',', pos)) >= 0) {
            	if (colIndex > headersize) {
            		String msg = "line number "+(count+1)+" has wrong number of arguments. num="+cols.length+" expected was="+headers.size()+" based on the headers";
    				if(cols.length == 1 && "".equals(cols[0].trim()))
    					msg = "line number="+(count+1)+" contains no columns(no commas) so we are skipping the line";
    				reportError(request, msg);
    				return;
            	}
                cols[colIndex]=row.substring(pos, end);
                pos = end + 1;
                colIndex++;
            }
            if (pos < row.length())
            	cols[colIndex]=row.substring(pos, row.length());
            
			count++;
			//play.Logger.info("processing the string "+row+" count is "+count);
			if((count%BATCH_SIZE) == 0) {
				noErrorCount+=((count-1)%BATCH_SIZE)+1;
				request.args.put("total", noErrorCount);
				waitForCatchingUp(request);
				//play.Logger.info("calling into the executor with "+batch.size()+" items, row is "+row);
				ExecutorsSingleton.executor.execute(new SaveBatch(tableMeta, headers, batch, request, ((count-1)%BATCH_SIZE)+1));
				batch = new Line[BATCH_SIZE];
				initializeBatch();
			}

			if(log.isDebugEnabled() && count % 2000 == 0)
				log.debug("Another 2000 rows was written.  total="+count);
		}

		private void waitForCatchingUp(Request request) {
			synchronized(request) {
				while(behind(request)) {
					try {
						Integer total = (Integer) request.args.get("total");
						Integer count = (Integer) request.args.get("count");
						if (log.isDebugEnabled())
			        		log.debug("We are behind. total="+total+" cnt="+count+" so we WAIT");
						request.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		private boolean behind(Request request) {
			Integer total = (Integer) request.args.get("total");
			Integer count = (Integer) request.args.get("count");
			int numRowsDiff = total - count;
			if (log.isDebugEnabled())
				log.debug("checking if behind, total ="+total+", count ="+count);
			if(numRowsDiff > BATCH_SIZE * 40) {
				//Allow 10 batches before we are officially behind
				return true;
			}
			return false;
		}

		private void readHeaders(String[] cols, Request request) {
			SecureTable sTable = (SecureTable) request.args.get("table");
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
			this.headersize = headers.size();
			initializeBatch();
		}

		@Override
		public void startingFormParameter(String headers, Request request,
				Response response) {
			if(headers.contains("name=\"file\""))
				readingFile = true;
			else
				readingFile = false;
			if (log.isDebugEnabled())
				log.debug("headers in body="+headers);
		}

		@Override
		public void currentFormParamComplete(Request request, Response resp) {
			//jsc comment this whole block for 100x
			if(readingFile && (count%BATCH_SIZE > 0)) {
				noErrorCount+=count%BATCH_SIZE;
				request.args.put("total", noErrorCount);
				ExecutorsSingleton.executor.execute(new SaveBatch(tableMeta, headers, batch, request, count%BATCH_SIZE));
				//batch = new ArrayList<Line>();
				if (log.isDebugEnabled())
					log.debug("Another "+count%BATCH_SIZE+" rows was written.  total="+count);
			}
			readingFile = false;
		}
	}

}
