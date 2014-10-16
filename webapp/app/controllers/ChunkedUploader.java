package controllers;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import play.Play;
import server.DatabusStreamChunkAggregator;

public class ChunkedUploader implements DatabusStreamChunkAggregator.ChunkedListener {
	protected ChunkConsumingThread consumer;
	protected boolean registered;
	
	@Override
	public boolean shouldNotify(String uri) {
		if (uri != null && uri.contains("api/postcsv"))
			return true;
		return false;
	}

	@Override
	public void chunk(File sharedFile, String uri) {
		if (!registered) {
			
			registered = true;
		}
		
		if (consumer == null) {
			uri = StringUtils.stripEnd(uri, "/");
			String table = StringUtils.substringAfterLast(uri, "/");
			consumer = new ChunkConsumingThread(sharedFile, table);
			Thread consumerThread = new Thread(consumer);
			consumerThread.setContextClassLoader(Play.classloader);
			consumer.setMyThread(consumerThread);
			consumerThread.start();
		}
		consumer.moreData();
	}

	@Override
	public void dataComplete() {
		consumer.setDataComplete(true);
		consumer.moreData();
	}	
}

