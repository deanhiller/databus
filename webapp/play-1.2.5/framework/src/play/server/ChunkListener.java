package play.server;

import java.io.ByteArrayInputStream;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public interface ChunkListener {

	void startingFormParameter(String headers, Request request, Response response);
	
	void moreFormParamData(ByteArrayInputStream in, Request request, Response response);

	void currentFormParamComplete(Request request, Response resp);

}
