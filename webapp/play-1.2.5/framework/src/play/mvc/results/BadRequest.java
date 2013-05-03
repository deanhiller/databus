package play.mvc.results;

import java.io.StringReader;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 400 Bad Request
 */
public class BadRequest extends Result {

	public BadRequest(String msg) {
		super(msg);
	}

	public BadRequest() {
	}

	@Override
    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.BAD_REQUEST;
        
        try {
        	String msg = getMessage();
        	byte[] data = msg.getBytes();
			response.out.write(data);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
