package controllers;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import play.Play;
import server.DatabusStreamChunkAggregator;

public abstract class ChunkedUploader implements DatabusStreamChunkAggregator.ChunkedListener {
	protected ChunkConsumingThread consumer;

	@Override
	public void chunk(File sharedFile, String uri) {
		
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

