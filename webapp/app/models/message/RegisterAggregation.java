package models.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This is a registration message
 * @author mcottere
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RegisterAggregation {
	
	@JsonProperty("name")
	@XmlElement(name="name")
    public String name;

	@JsonProperty("group")
    @XmlElement(name="group")
	private String group;
	
	@JsonProperty("database")
    @XmlElement(name="database")
	private String database;
	
	@JsonProperty("urls")
    @XmlElement(name="urls")
    public List<String> urls;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

} 
