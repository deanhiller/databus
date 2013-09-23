package controllers.gui;

import models.message.StreamModule;

public class StreamTuple {

	private StreamModule stream;
	private String path;

	public StreamTuple(StreamModule currentStr, String path) {
		this.stream = currentStr;
		this.path = path;
	}

	public StreamModule getStream() {
		return stream;
	}

	public void setStream(StreamModule stream) {
		this.stream = stream;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	

}
