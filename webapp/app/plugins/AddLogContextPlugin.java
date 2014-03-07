package plugins;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;

import controllers.SecurityUtil;
import controllers.gui.auth.GuiSecure;

import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router.Route;
import play.mvc.Scope.Session;
import play.vfs.VirtualFile;

public class AddLogContextPlugin extends PlayPlugin {

	private static final Logger log = LoggerFactory.getLogger(AddLogContextPlugin.class);
	private static long lastTimeStamp;
	private boolean isProduction;
	private ThreadLocal<Boolean> isStartupBean = new ThreadLocal<Boolean>();

	public AddLogContextPlugin() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		String mode = Play.configuration.getProperty("application.mode");
		if("prod".equals(mode)) {
			isProduction = true;
		}
	}
	static {
		LocalDateTime time = new LocalDateTime(2013, 8, 1, 0, 0);
		long baseTime = time.toDateTime().getMillis();
		lastTimeStamp = (System.currentTimeMillis() - baseTime)*1000;
	}
	
	private synchronized long fetchLast() {
		return ++lastTimeStamp;
	}
	
	@Override
	public void routeRequest(Request request) {
		isStartupBean.set(false);		
		beginRequest();
		super.routeRequest(request);
	}

	private void beginRequest() {
		MDC.put("filter", "false");
		Request current = Request.current();
		if(current != null) {
			long start = System.currentTimeMillis();
			current.args.put("__startTime", start);
			overrideMDC(current, fetchLast());
			if(isProduction && log.isInfoEnabled())
				log.info("---begin request="+current.method+":"+current.path);
			else if(log.isInfoEnabled() || (!current.path.startsWith("/public") && log.isInfoEnabled()))
				log.info("---begin request="+current.method+":"+current.path);
		}
	}

	private void overrideMDC(Request request, long txId) {
		Session session = Session.current();
		String username = request.user;
		if(session != null) {
			username = SecurityUtil.getUser();
		}

		if(request.user != null) {
			if(username == null)
				username = "("+request.user+")";
			else
				username += "("+request.user+")";
		}

		request.args.put("__txId", txId);
		MDC.put("txId", ""+txId);

		if(username == null)
			return;

		//they are doing basic auth so add the user in basic auth to the logs
		request.args.put("__user", username);
		MDC.put("user", username);
		if(username.contains("(robot-") && request.path.startsWith("/api/postdata"))
			MDC.put("filter", "true");//user is robot AND post so override user			
	}

	@Override
	public boolean serveStatic(VirtualFile file, Request request,
			Response response) {
		beginRequest();
		return super.serveStatic(file, request, response);
	}

	@Override
	public void beforeInvocation() {
		Request current = Request.current();
		if(current != null) {
			Long txId = (Long) current.args.get("__txId");
			if(txId != null)
				overrideMDC(current, txId);
		}
		super.beforeInvocation();
	}

	@Override
	public void invocationFinally() {
		Boolean isStartup = isStartupBean.get();
		if(isStartup != null && isStartup)
			return; //we can't execute invocation Finally when play is executing startup beans
		isStartupBean.set(null);

		Response resp = Response.current();
		Request current = Request.current();
		if(current != null) {
			Boolean isLastCall = (Boolean) current.args.get("__lastCall");
			if(resp!=null && (!resp.chunked || (isLastCall != null && isLastCall)))
				processEndOfRequestMaybe(current);
		}
		
		//When this thread returns, it can process a completely new request so reset all the MDC stuff since we don't want
		//the playframework logging stuff with these variables when it may be for a completely different request then the MDC
		//variables we just had in there
		MDC.put("txId", "");
		MDC.put("user", "");
		MDC.put("filter", "false");
	}

	private void processEndOfRequestMaybe(Request current) {
		Boolean isContinuation = (Boolean) current.args.get("__continuations");
		if(isContinuation == null) {
			Long start = (Long) current.args.get("__startTime");
			if(start == null)
				return; //for static resources

			//start will be null IF StartupBean was just invoked!!!! 
			long total = System.currentTimeMillis() - start;
			if (log.isInfoEnabled()) {
				if(isProduction && log.isInfoEnabled())
					log.info("---ended request="+current.method+":"+current.path+" total time="+total+" ms");
				else if(log.isInfoEnabled() || (!current.path.startsWith("/public") && log.isInfoEnabled()))
					log.info("---ended request="+current.method+":"+current.path+" total time="+total+" ms");
			}
		}
	}
}
