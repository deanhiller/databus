package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 302 Redirect
 */
public class Redirect extends Result {

    public String url;
    public int code = Http.StatusCode.FOUND;

    public Redirect(String url) {
        this.url = url;
    }

    public Redirect(String url,boolean permanent) {
        this.url = url;
        if (permanent)
            this.code = Http.StatusCode.MOVED;
    }

    public Redirect(String url,int code) {
        this.url = url;
        this.code=code;
    }

    public void apply(Request request, Response response) {
        try {
        	
        	//NOTE: The below code BREAKS login/logout or any redirect for that matter in the situation where you 
        	//1. have playframework running http
        	//2. are running an LB that is doing the https
        	//The code below incorrectly assumes we are doing http redirecting to an http address on login when it should stay on https.
        	//
//            if (url.startsWith("http")) {
//                //
//            } else if (url.startsWith("/")) {
//                url = String.format("http%s://%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, url);
//            } else {
//                url = String.format("http%s://%s%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, request.path, request.path.endsWith("/") ? url : "/" + url);
//            }
            response.status = code;
            response.setHeader("Location", url);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
