package controllers.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.message.PostTrigger;

public class TriggerContext {

	private static final Logger log = LoggerFactory.getLogger(TriggerContext.class);

	private Map<String, String> row;
	private PostTrigger trigger;

	public TriggerContext(Map<String, String> jsonRow, PostTrigger trigger) {
		this.row = jsonRow;
		this.trigger = trigger;
	}

	public Map<String, String> getRow() {
		return row;
	}

	public String fireCallback(String url) {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		HttpUriRequest request = new HttpGet(url);
		try {
			HttpResponse resp = httpclient.execute(request );
			InputStream in = resp.getEntity().getContent();
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			String body = writer.toString();
			return body;
		} catch (Exception e) {
			log.warn("Exception firing trigger="+trigger.getTable()+" db="+trigger.getDatabase(), e);
		}

		return "ERROR";
	}
}
