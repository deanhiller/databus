package play.server;

import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;

public class OurChunk {

	private HttpChunk chunk;
	private HttpMessage originalRequest;

	public OurChunk(HttpChunk chunk, HttpMessage currentMessage) {
		this.chunk = chunk;
		this.originalRequest = currentMessage;
	}

	public HttpChunk getChunk() {
		return chunk;
	}

	public HttpMessage getOriginalRequest() {
		return originalRequest;
	}
	

}
