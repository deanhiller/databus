package play.server;

import java.util.Hashtable;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Invoker;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.server.PlayHandler.NettyInvocation;

public class Blackboard {

	private static final Logger log = LoggerFactory.getLogger(Blackboard.class);

	private static Map<HttpRequest, ReqState> requestToList = new Hashtable<HttpRequest, ReqState>();

	public static void invoke(ChannelHandlerContext ctx, Request request, Response response, MessageEvent msg, NettyInvocation nettyInvoc) {
		HttpRequest req = nettyInvoc.getNettyRequest();
		ReqState state = new ReqState(ctx, request, response);

		requestToList.put(req, state);

		//Invoke since this is the first chunk of this request
		Invoker.invoke(nettyInvoc);
	}

	public static void invoke(MessageEvent messageEvent, OurChunk c, PlayHandler handler) {
		HttpRequest msg = (HttpRequest) c.getOriginalRequest();
		ReqState reqState = requestToList.get(msg);
		if(reqState.getRequest().chunkFailure != null)
			return; //in failure mode, no need to process
		try {
			synchronized(msg) {
				while(!reqState.isInitialized()) {
					try {
						msg.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
			
			reqState.push(c.getChunk());
			
			return;
		} catch(Exception e) {
			//record failure that the controller uses later...
			reqState.getRequest().chunkFailure = e;
			if (log.isWarnEnabled())
        		log.warn("Exception processing chunk", e);
			return;
		}
	}

	public static void markAsComplete(HttpRequest req) {
		ReqState state = requestToList.get(req);
		synchronized(req) {
			state.setInitialized(true);
			state.getRequest().isNew = false;
			state.initChunkProcessor();
			req.notifyAll();
		}
	}

	public static void invokeLast(ChannelHandlerContext ctx, MessageEvent messageEvent, OurChunk chunk, PlayHandler handler) {
		HttpRequest msg = (HttpRequest) chunk.getOriginalRequest();
		ReqState reqState = requestToList.remove(msg);
		Request request = reqState.getRequest();
		Response response = reqState.getResponse();
		NettyInvocation nettyInvoc = handler.createInvocation(request, response, ctx, msg, messageEvent);
		Invoker.invoke(nettyInvoc);
	}

}
