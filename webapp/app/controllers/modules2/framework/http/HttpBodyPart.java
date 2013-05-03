package controllers.modules2.framework.http;

public class HttpBodyPart extends AbstractHttpMsg {

	private byte[] data;

	public HttpBodyPart(byte[] data, String url, HttpListener listener) {
		super(url, listener);
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

}
