package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;
import com.alvazan.play.NoSql;

import models.PermissionType;
import models.SecureTable;
import controllers.SecurityUtil;
import controllers.gui.util.ExecutorsSingleton;
import controllers.gui.util.Line;
import play.Play;
import play.mvc.Http.WebSocketClose;
import play.mvc.Http.WebSocketEvent;
import play.mvc.Http.WebSocketFrame;
import play.mvc.WebSocketController;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Unauthorized;

public class FileUploadSocket extends WebSocketController {

	private static final Logger log = LoggerFactory.getLogger(FileUploadSocket.class);
	static final int ERROR_LIMIT = 10;
	private static ThreadPoolExecutor executor;
	private static int numThreads = 10;
	private static final ObjectMapper mapper = new ObjectMapper();
	private static long startTime = 0;
	
	static {
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(1000);
		String configurednumthreads = Play.configuration.getProperty("socket.upload.num.threads");
		if (StringUtils.isNotBlank(configurednumthreads))
			numThreads = Integer.parseInt(configurednumthreads);
		executor = new ThreadPoolExecutor(numThreads, numThreads, 5, TimeUnit.MINUTES, queue, new OurRejectHandler());
	}
	
	public static void fileToWebSocket(String table) throws IOException {
		startTime=System.currentTimeMillis();
		if (request.isNew) {
			PermissionType p = SecurityUtil.checkSingleTable2(table);
			if (PermissionType.READ_WRITE.isHigherRoleThan(p))
				throw new Unauthorized(
						"You do not have a high enough permission to write to this table="
								+ table);

			SecureTable sdiTable = SecureTable.findByName(NoSql.em(), table);
			DboTableMeta meta = sdiTable.getTableMeta();
			meta.initCaches();
			Collection<DboColumnMeta> columns = meta.getAllColumns();
			for (DboColumnMeta m : columns) {
				// force load from noSQL store right now...
				m.getColumnName();
			}

			NoSqlEntityManagerFactory factory = NoSql.getEntityManagerFactory();
			request.args.put("state", new SocketStateCSV(factory, sdiTable, executor, outbound));
		}

		SocketStateCSV state = (SocketStateCSV) request.args.get("state");
		while (inbound.isOpen()) {
			WebSocketEvent e = await(inbound.nextEvent());
			if (e instanceof WebSocketFrame) {
				WebSocketFrame frame = (WebSocketFrame) e;
				if(state.getErrors().size() > 0) {
					log.warn("odd, socket is always closed right after error so how did we get here...possibly a race condition we don't care much about");
					continue; 
				} else if (frame.isBinary) {
					processFile(state, frame);
				} else if(frame.textData.startsWith("json")) {
					String jsonStr = frame.textData.substring(4);
					Map json = mapper.readValue(jsonStr, Map.class);
					String action = (String) json.get("action");
					if(action.equals("start")) {
						int size = (Integer) json.get("size");
						state.setSize(size);
						outbound.send("{ \"start\":true }");
						log.info("hi there");
					} else if(action.equals("cancel")) {
						log.info("user cancelled upload");
						state.getErrors().add("User cancelled file upload");
						disconnect();
					}
				} else if(frame.textData.equalsIgnoreCase("end")) {
					endOfData(state);
				} else if (frame.textData.equals("quit")) {
					outbound.send("Bye!");
					disconnect();
				}
			}
			if (e instanceof WebSocketClose) {
				log.info("Socket closed for table="
						+ state.getSdiTable().getName());
			}
		}
		log.info("total time for web socket upload:  "+(System.currentTimeMillis()-startTime));
	}
	
	public static void fileToWebSocketJSON(String table) throws IOException {
		if (request.isNew) {
			PermissionType p = SecurityUtil.checkSingleTable2(table);
			if (PermissionType.READ_WRITE.isHigherRoleThan(p))
				throw new Unauthorized(
						"You do not have a high enough permission to write to this table="
								+ table);

			NoSqlEntityManagerFactory factory = NoSql.getEntityManagerFactory();
			SecureTable sdiTable = SecureTable.findByName(NoSql.em(), table);
			SocketState state = new SocketStateJSON(factory, sdiTable, executor, outbound);

			if (sdiTable == null){
				request.args.put("state", state);
				if (outbound != null)
					state.reportErrorAndClose("No such table '"+table+"'");
				else 
					throw new Unauthorized("No such table '"+table+"'");
				log.info("user selected a non-existant table:'"+table+"'...");
				return;
			}
			DboTableMeta meta = sdiTable.getTableMeta();
			meta.initCaches();
			Collection<DboColumnMeta> columns = meta.getAllColumns();
			for (DboColumnMeta m : columns) {
				// force load from noSQL store right now...
				m.getColumnName();
			}

			request.args.put("state", state);
		}

		SocketStateJSON state = (SocketStateJSON) request.args.get("state");
		while (inbound.isOpen()) {
			WebSocketEvent e = await(inbound.nextEvent());
			if (e instanceof WebSocketFrame) {
				WebSocketFrame frame = (WebSocketFrame) e;
				if(state.getErrors().size() > 0) {
					log.warn("odd, socket is always closed right after error so how did we get here...possibly a race condition we don't care much about");
					continue; 
				} else if (frame.isBinary) {
					processFile(state, frame);
				} else if(frame.textData.startsWith("json")) {
					String jsonStr = frame.textData.substring(4);
					Map json = mapper.readValue(jsonStr, Map.class);
					String action = (String) json.get("action");
					if(action.equals("start")) {
						int size = (Integer) json.get("size");
						state.setSize(size);
						outbound.send("{ \"start\":true }");
						log.info("hi there");
					} else if(action.equals("cancel")) {
						log.info("user cancelled upload");
						state.getErrors().add("User cancelled file upload");
						disconnect();
					}
				} else if(frame.textData.equalsIgnoreCase("end")) {
					log.info("got 'end', calling endOfData!");
					endOfData(state);
				} else if (frame.textData.equals("quit")) {
					outbound.send("Bye!");
					log.info("got 'quit', disconnecting!");
					disconnect();
				}
			}
			if (e instanceof WebSocketClose) {
				log.info("Socket closed for table="
						+ state.getSdiTable().getName());
			}
		}
	}

	
	private static void reportErrorAndClose(SocketState state, String msg) {
		state.reportErrorAndClose(msg);
	}
	
	private static void processFile(SocketState state, WebSocketFrame frame)
			throws IOException {
		try {
			byte[] binaryData = frame.binaryData;
			String data = new String(binaryData);
			state.processFile(data);
		} catch(Exception e) {
			log.warn("Exception uploading file", e);
			reportErrorAndClose(state, "1. Exception uploading.  exc="+e+" msg="+e.getMessage());
		}
	}

	private static void endOfData(SocketState state) {
		try {
			state.endOfData();
		} catch(Exception e) {
			log.warn("Exception uploading file", e);
			reportErrorAndClose(state, "2. Exception uploading.  exc="+e+" msg="+e.getMessage());
		}
	}

}
