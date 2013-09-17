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

	@JsonProperty("database")
	@XmlElement(name="database")
    public String database;

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

	@JsonProperty("lastRunTime")
    @XmlElement(name="lastRunTime")
	private long lastRunTime;
	
	@JsonProperty("lastRunDuration")
    @XmlElement(name="lastRunDuration")
	private long lastRunDuration;
	
	@JsonProperty("lastException")
    @XmlElement(name="lastException")
	private String lastException;
	

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

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
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
	
	public long getLastRunTime() {
		return lastRunTime;
	}

	public void setLastRunTime(long lastRunTime) {
		this.lastRunTime = lastRunTime;
	}

	public long getLastRunDuration() {
		return lastRunDuration;
	}

	public void setLastRunDuration(long lastRunDuration) {
		this.lastRunDuration = lastRunDuration;
	}

	public String getLastException() {
		return lastException;
	}

	public void setLastException(String lastException) {
		this.lastException = lastException;
	}

	@Override
	public String toString() {
		return "Trigger [id=" + id + ", url=" + url + ", rate=" + rate
				+ ", offset=" + offset + ", before=" + before + ", after="
				+ after + ", runAsUser=" + runAsUser + ", lastRunSuccess="
				+ lastRunSuccess + ", lastRunTime=" + lastRunTime
				+ ", lastRunDuration=" + lastRunDuration + ", lastError="
				+ lastException + "]";
	}

} // Register
