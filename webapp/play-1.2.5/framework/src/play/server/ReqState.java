package play.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class ReqState {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ReqState.class);
	private ChannelHandlerContext ctx;
	private Request request;
	private HttpChunkStream processor;
	private boolean controllerInitialized;
	private int counter = 0;
	private Response response;

	public ReqState(ChannelHandlerContext ctx, Request request, Response response) {
		this.ctx = ctx;
		this.request = request;
		this.response = response;

		processor = createProcessor(request, response);
	}

	public static HttpChunkStream createProcessor(Request request, Response response) {
		Header header = request.headers.get("content-type");
		String value = header.value();
		String[] values = value.split(";");
		//For multipart, we need to parse out each payload in the body otherwise we can use a thin wrapper to stream the body through since there
		//is nothing else
		if("multipart/form-data".equals(values[0].trim().toLowerCase()))
			return new MultiPartStream(request, response, values);
		else
			return new EntireBodyStream(request, response, values);
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public Request getRequest() {
		return request;
	}

	public void push(HttpChunk chunk) {
		ChannelBuffer buffer = chunk.getContent();
		byte[] data = new byte[buffer.readableBytes()];
		
		buffer.readBytes(data);
		
		if(log.isDebugEnabled() && counter < 10) {
			String s = new String(data);
			log.info("counter="+counter+" partial chunk="+s);
			counter++;
		}
		
		processor.addMoreData(data, chunk.isLast());
	}

	public void initChunkProcessor() {
		ChunkListener chunkListener = request.chunkListener;
		processor.setChunkListener(chunkListener);
	}

	public void setInitialized(boolean initialized) {
		controllerInitialized = initialized;
	}

	public boolean isInitialized() {
		return controllerInitialized;
	}

	public Response getResponse() {
		return response;
	}

}
