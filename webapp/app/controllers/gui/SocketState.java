package controllers.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import models.SecureTable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Outbound;

import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z8spi.meta.DboColumnIdMeta;
import com.alvazan.orm.api.z8spi.meta.DboColumnMeta;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

import controllers.gui.util.Line;

public abstract class SocketState extends PlayPlugin {

	protected static final Logger log = LoggerFactory.getLogger(SocketStateCSV.class);
	protected static int BATCH_SIZE = 5000;
	static final int ERROR_LIMIT = 10;
	protected NoSqlEntityManagerFactory factory;
	protected SecureTable sdiTable;
	protected ExecutorService executor;
	protected Outbound outbound;
	private int fileSize;
	protected StringBuffer buffer = new StringBuffer();
	protected List<Line> batch = new ArrayList<Line>();
	protected DboTableMeta tableMeta;
	private int doneCount = 1;
	private int charactersDone;
	private List<String> errors = new ArrayList<String>();

	public SocketState() {
		super();
		String configuredbatchsize = Play.configuration.getProperty("socket.upload.batch.size");
		if (StringUtils.isNotBlank(configuredbatchsize))
			BATCH_SIZE = Integer.parseInt(configuredbatchsize);
	}

	protected abstract void processFile(String s) throws IOException; 
	public abstract int getNumItemsSubmitted();

	public SecureTable getSdiTable() {
		return sdiTable;
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
	
	public void setTotalCharactersDone(int newCharCount) {
		charactersDone = newCharCount;
	}

	public int getCharactersDone() {
		return charactersDone;
	}

	public void endOfData() {
		//execute the last batch
		executor.execute(new SaveBatch(tableMeta, batch, this, outbound));
	}

	public int getFileSize() {
		return fileSize;
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
		newMsg = newMsg.replace("\n", " ").replace("\r", " ");
		if(!outbound.isOpen())
			return;
		outbound.send("{ \"error\":\""+newMsg+"\" }");
		outbound.close();
	}
	

}