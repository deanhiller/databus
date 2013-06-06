package play.server;

import java.io.ByteArrayInputStream;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class EntireBodyStream implements HttpChunkStream {

	private Request request;
	private Response response;
	private ChunkListener listener;
	private boolean firstDelivered = false;
	
	public EntireBodyStream(Request request, Response response, String[] values) {
		this.request = request;
		this.response = response;
	}

	@Override
	public void addMoreData(byte[] data, boolean isLast) {
		if(!firstDelivered) {
			firstDelivered = true;
			listener.startingFormParameter("", request, response);
		}
		
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		listener.moreFormParamData(in, request, response);
		
		if(isLast)
			listener.currentFormParamComplete(request, response);
	}

	@Override
	public void setChunkListener(ChunkListener chunkListener) {
		this.listener = chunkListener;
	}

}
