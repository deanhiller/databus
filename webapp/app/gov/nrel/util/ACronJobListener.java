package gov.nrel.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;

import models.EntityUser;
import models.SecureSchema;
import models.message.Trigger;

import org.apache.commons.lang.StringUtils;
import org.mortbay.log.Log;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.CronServiceFactory;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Realm.AuthScheme;

import controllers.api.CronJobController;
import controllers.modules2.framework.ProductionModule;
import controllers.modules2.framework.chain.AHttpChunkingListener;
import controllers.modules2.framework.http.HttpListener;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.RemoteProcessor.AuthInfo;
import controllers.modules2.framework.translate.TranslationFactory;

public class ACronJobListener {

	private static final Logger log = LoggerFactory.getLogger(ACronJobListener.class);
	private Long startOverride;
	private Long endOverride;
	private CronService svc;
	private AsyncHttpClient client = ProductionModule.createSingleton();

	public ACronJobListener(CronService svc) {
		this.svc = svc;
	}

	public ACronJobListener(CronService svc, long startOverride, long endOverride) {
		this.svc = svc;
		this.startOverride = startOverride;
		this.endOverride = endOverride;
	}

	public void monitorFired(PlayOrmCronJob cronJob) {
		Trigger trigger = transform(cronJob);
		if(log.isInfoEnabled())
			log.info("monitor firing for url="+trigger.getUrl());
		String url = trigger.getUrl();

		boolean success = false;
		String exceptionString = "none";
		//synchronously call the url awaiting a result here

		String db = trigger.getDatabase();
		String id = trigger.getId();
		Long start = startOverride;
		Long end = endOverride;
		if(startOverride == null) {
			long time = System.currentTimeMillis();
			long range = time - trigger.getOffset();
			long multiplier = range / trigger.getRate();
			long endLastPeriod = trigger.getOffset()+multiplier*trigger.getRate();
			if(endLastPeriod > time) {
				log.warn("WE have a problem in that the window is in the future...this is our bug in that our trigger fired tooooo early then. db="+db+" cronjob="+id, new RuntimeException("trigger fired too early.  look next log"));
			}
			long beginLastPeriod = endLastPeriod - trigger.getRate();
			start = beginLastPeriod - trigger.getBefore();
			end = endLastPeriod+trigger.getAfter();
			while(end > time) {
				start -= trigger.getRate();
				end -= trigger.getRate();
				if(log.isInfoEnabled())
					log.info("db="+db+" cron="+id+"time="+time+" range="+range+" multiplier="+multiplier+" endLastPeriod="+endLastPeriod+" beginLastPer="+beginLastPeriod+" start="+start+" end="+end+" trigger="+trigger);
			}
		}

		try {
			fireHttpRequestAndWait(cronJob, url, start, end);
			success = true;
		} catch(RuntimeException e) {
			log.warn("Exception on cron job requesting url. triggerid="+cronJob.getId()+" trigger="+trigger, e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			exceptionString = sw.toString();
			//MUST be thrown as this is called from controller and we must give user exception if his url does not work at all
			throw e;
		} finally {
			cronJob.getProperties().put("success", success+"");
			cronJob.getProperties().put("lastException", exceptionString);
			svc.saveMonitor(cronJob);
		}
	}

	private void fireHttpRequestAndWait(PlayOrmCronJob m, String url, long start, long end) {
		String username = m.getProperties().get("userName");
		String apiKey = m.getProperties().get("apiKey");
		// fix this so it is passed in instead....
		Realm realm = new Realm.RealmBuilder()
				.setPrincipal(username)
				.setPassword(apiKey)
				.setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
				.build();

		String fullUrl = url+"/"+start+"/"+end;

		if (log.isInfoEnabled())
			log.info("CRON JOB-sending request to url=" + fullUrl);
		RequestBuilder b = new RequestBuilder("GET").setUrl(fullUrl)
				.setRealm(realm);
		com.ning.http.client.Request httpReq = b.build();

		try {
			MyHandler handler = new MyHandler();
			long startTime = System.currentTimeMillis();
			long duration = 0;
			m.getProperties().put("startTime", ""+startTime);
			m.getProperties().put("duration", ""+duration);
			client.executeRequest(httpReq, handler);
			duration = startTime - System.currentTimeMillis();
			m.getProperties().put("duration", ""+duration);
			handler.checkStatus();
		} catch(IOException e) {
			throw new RuntimeException("Failed to send request. url="+url);
		}
	}

	public static String formId(String origId) {
		String id = "_log"+origId;
		return id;
	}
	
	public static PlayOrmCronJob transform(Trigger msg, String id,
			SecureSchema schemaDbo, EntityUser user) {
		PlayOrmCronJob job;
		job = new PlayOrmCronJob();
		job.setId(id);
		job.setType("trigger");
		job.setTimePeriodMillis(msg.getRate());
		//offset needs to be changed to offset+after so that we always fire after the after window
		job.setEpochOffset(msg.getOffset()+msg.getAfter());
		job.getProperties().put("offset", msg.getOffset()+"");
		job.getProperties().put("before", msg.getBefore()+"");
		job.getProperties().put("after", msg.getAfter()+"");
		job.getProperties().put("schemaId", schemaDbo.getId());
		job.getProperties().put("url", msg.getUrl());
		job.getProperties().put("userId", user.getId());
		job.getProperties().put("userName", user.getUsername());
		job.getProperties().put("apiKey", user.getApiKey());
		job.getProperties().put("database", schemaDbo.getSchemaName());
		return job;
	}

	public static Trigger transform(PlayOrmCronJob job) {
		Trigger t = new Trigger();
		//trim off the 4 prefix we put on...
		t.setId(job.getId().substring(4));
		t.setRate(job.getTimePeriodMillis());
		t.setOffset(CronJobController.toLong(job.getProperties().get("offset")));
		t.setBefore(CronJobController.toLong(job.getProperties().get("before")));
		t.setAfter(CronJobController.toLong(job.getProperties().get("after")));
		t.setUrl(job.getProperties().get("url"));
		t.setRunAsUser(job.getProperties().get("userName"));
		t.setLastRunSuccess(toBoolean(job.getProperties().get("success")));
		t.setDatabase(job.getProperties().get("database"));
		if (StringUtils.isNotBlank(job.getProperties().get("startTime")))
			t.setLastRunTime(Long.parseLong(job.getProperties().get("startTime")));
		if (StringUtils.isNotBlank(job.getProperties().get("duration")))
			t.setLastRunDuration(Long.parseLong(job.getProperties().get("duration")));
		t.setLastError(job.getProperties().get("lastException"));
		return t;
	}

	private static boolean toBoolean(String val) {
		if("true".equals(val))
			return true;
		return false;
	}

	private static class MyHandler implements AsyncHandler<Object> {
		private Throwable failure;
		private HttpResponseStatus status;
		private boolean complete;
		private ByteBuffer buffer = ByteBuffer.allocate(5000); 
		
		@Override
		public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart part) throws Exception {
			byte[] data = part.getBodyPartBytes();
			if(buffer.remaining() > data.length)
				buffer.put(data);
			return AsyncHandler.STATE.CONTINUE;
		}

		public void checkStatus() {
			synchronized(this) {
				while(!complete && failure == null) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}

			if(failure != null) {
				String dataMsg = read();
				throw new RuntimeException("Failure.  first 5000 bytes of body="+dataMsg+"(END)\n", failure);
			} else if(status.getStatusCode() != 200) {
				String dataMsg = read();
				throw new RuntimeException("web url returned="+status.getStatusCode()+" first 5000 bytes of body message="+dataMsg+"(END)\n");
			}
		}

		private String read() {
			buffer.flip();
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			String s = new String(data);
			return s;
		}

		@Override
		public Object onCompleted() throws Exception {
			synchronized(this) {
				this.complete = true;
				this.notifyAll();
			}
			return null;
		}

		@Override
		public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(
				HttpResponseHeaders arg0) throws Exception {
			return AsyncHandler.STATE.CONTINUE;
		}

		@Override
		public com.ning.http.client.AsyncHandler.STATE onStatusReceived(
				HttpResponseStatus status) throws Exception {
			this.status = status;
			return AsyncHandler.STATE.CONTINUE;
		}

		@Override
		public void onThrowable(Throwable t) {
			failure = t;
			synchronized(this) {
				this.notifyAll();
			}
		}
	}
}
