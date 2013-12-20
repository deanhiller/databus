package controllers.gui;

public class Snapshot {

	private String before;
	private String payload;
	private String after;

	public Snapshot(String bufferBefore, String s, String buffer) {
		this.before = bufferBefore;
		this.payload = s;
		this.after = buffer;
	}

	public String getBefore() {
		return before;
	}

	public String getPayload() {
		return payload;
	}

	public String getAfter() {
		return after;
	}
	
}
