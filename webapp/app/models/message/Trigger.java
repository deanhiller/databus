package models.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Trigger {
	
	@JsonProperty("id")
	@XmlElement(name="id")
    public String id;

	@JsonProperty("url")
    @XmlElement(name="url")
    public String url;	
	
	@JsonProperty("rate")
    @XmlElement(name="rate")
    public long rate;
	
	@JsonProperty("offset")
    @XmlElement(name="offset")
	private long offset;
	
	@JsonProperty("before")
    @XmlElement(name="before")
	private long before;
	
	@JsonProperty("after")
    @XmlElement(name="after")
	private long after;

	@JsonProperty("runAsUser")
    @XmlElement(name="runAsUser")
	private String runAsUser;

	@JsonProperty("lastRunSuccess")
    @XmlElement(name="lastRunSuccess")
	private boolean lastRunSuccess;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getRate() {
		return rate;
	}

	public void setRate(long rate) {
		this.rate = rate;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public long getBefore() {
		return before;
	}

	public void setBefore(long before) {
		this.before = before;
	}

	public long getAfter() {
		return after;
	}

	public void setAfter(long after) {
		this.after = after;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRunAsUser() {
		return runAsUser;
	}

	public void setRunAsUser(String user) {
		this.runAsUser = user;
	}

	public boolean isLastRunSuccess() {
		return lastRunSuccess;
	}

	public void setLastRunSuccess(boolean lastRunSuccess) {
		this.lastRunSuccess = lastRunSuccess;
	}

} // Register
