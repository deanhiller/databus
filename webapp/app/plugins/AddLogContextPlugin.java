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
import play.mvc.Scope.Session;
import play.vfs.VirtualFile;

public class AddLogContextPlugin extends PlayPlugin {

	private static final Logger log = LoggerFactory.getLogger(AddLogContextPlugin.class);
	private static long lastTimeStamp;
	private boolean isProduction;
	private ThreadLocal<Long> startTime = new ThreadLocal<Long>();
	
	public AddLogContextPlugin() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		String mode = Play.configuration.getProperty("application.mode");
		if("prod".equals(mode)) {
			isProduction = true;
		}
	}
	static {
		LocalDateTime time = new LocalDateTime(2012, 6, 1, 0, 0);
		long baseTime = time.toDateTime().getMillis();
		lastTimeStamp = System.currentTimeMillis() - baseTime;
	}
	
	private synchronized long fetchLast() {
		return ++lastTimeStamp;
	}
	
	@Override
	public void routeRequest(Request request) {
		beginRequest();
		super.routeRequest(request);
	}

	private void beginRequest() {
		MDC.put("filter", "false");
		long start = System.currentTimeMillis();
		startTime.set(start);
		
		Session session = Session.current();
		if(session != null) {
			setupMDC(session);
		}
		
		Request current = Request.current();
		if(current != null) {
			overrideMDC(session, current);
			if (log.isInfoEnabled())
				if(isProduction || (!current.path.startsWith("/public")))
					log.info("---begin request="+current.method+":"+current.path);
		}
	}

	private void overrideMDC(Session session, Request request) {
		if(session == null)
			return;
		String username = SecurityUtil.getUser();
		String user = request.user;
		if(username != null) //a user is logged in so use the session user in the logs
			return;
		else if(user != null) {
			//they are doing basic auth so add the user in basic auth to the logs
			MDC.put("user", user);
			if(user.startsWith("robot") && request.path.startsWith("/api/postdata"))
				MDC.put("filter", "true");//basic auth with robot AND post so override user			
		}
	}

	private void setupMDC(Session session) {
		if(session.get("sid") == null) {
			session.put("sid", fetchLast()+"");
		}
		
		String sid = session.get("sid");
		String username = SecurityUtil.getUser();
		MDC.put("sessionid", sid);
		MDC.put("user", ""+username);
	}

	@Override
	public boolean serveStatic(VirtualFile file, Request request,
			Response response) {
		beginRequest();
		return super.serveStatic(file, request, response);
	}

	
	@Override
	public void invocationFinally() {
		Long start = startTime.get();
		//start will be null IF StartupBean was just invoked!!!! 
		if(start != null) {
			Request current = Request.current();
			long total = System.currentTimeMillis() - start;
			startTime.set(null);
			if (log.isInfoEnabled())
				if(isProduction || (!current.path.startsWith("/public")))
					log.info("---ended request="+current.method+":"+current.path+" total time="+total+" ms");
		}
		
		MDC.put("sessionid", "");
		MDC.put("user", "");
		MDC.put("filter", "false");
	}
	
}
