package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import models.SecureTable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.results.Unauthorized;

import com.alvazan.play.NoSql;

import controllers.gui.OurRejectHandler;
import controllers.gui.SocketState;
import controllers.gui.SocketStateCSV;



public class ChunkedUpload extends Controller {

	protected static final Logger log = LoggerFactory.getLogger(ChunkedUpload.class);

	public static Map<String, ChunkConsumingThread> registeredListeners = new HashMap<String, ChunkConsumingThread>();
	private static int numThreads = 10;

	static {
		String configurednumthreads = Play.configuration.getProperty("socket.upload.num.threads");
		if (StringUtils.isNotBlank(configurednumthreads))
			numThreads = Integer.parseInt(configurednumthreads);
	}

	public static void asyncPostDataCSV(String table) {
		postDataCSV(table, true);
	}
	
	public static void syncPostDataCSV(String table) {
		postDataCSV(table, false);
	}
	
	private static void postDataCSV(String table, boolean async) {
		InputStream in = Request.current().body;
		
		Header header = Request.current().headers.get("chunkedbufferfile");
		String chunkedKey = null;
		if (header != null && header.values != null)
			chunkedKey = header.value();
		
		if (chunkedKey != null) {
			if (!async) {
				ChunkConsumingThread asyncUploadingThread = registeredListeners.get(chunkedKey);
				asyncUploadingThread.setWaitingThread(Thread.currentThread());
				if (asyncUploadingThread != null) {
					while(!asyncUploadingThread.isProcessingComplete()) {
						LockSupport.parkNanos(10000000000l);  //thats 10 seconds.
						if (log.isDebugEnabled())
							log.debug("got through park() in csvupload, testing isProcessingComplete at "+System.currentTimeMillis());
					}
				}
				if (log.isDebugEnabled())
					log.debug("got through wait at "+System.currentTimeMillis());
			}
			//else, we can just return success without waiting for the async background thread to finish.
			if (log.isDebugEnabled())
				log.debug("ChunkedUpload already handled async it may have finished already, or might still be running");
		}
		else {
			try {
				SecureTable sdiTable;
				try {
					sdiTable = SecureTable.findByName(NoSql.em(), table);
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new Unauthorized("You do not have access to the table "+table+", or it does not exist.");
				}
				ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(1000), new OurRejectHandler());
				SocketState state = new SocketStateCSV(NoSql.getEntityManagerFactory(), sdiTable, executor, null);
				StringWriter writer = new StringWriter();
				IOUtils.copy(in, writer);
				state.processFile(writer.toString());
				state.endOfData();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}

	}		
}
