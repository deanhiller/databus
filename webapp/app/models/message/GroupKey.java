package models.message;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

public class GroupKey {

	@JsonProperty("modelName")
	@XmlElement(name="modelName")
	public String groupName;
	
	@JsonProperty("keyForGet")
	@XmlElement(name="keyForGet")
	public String keyForGet;

	@JsonProperty("keyForPost")
	@XmlElement(name="keyForPost")
	public String keyForPost;
	
	public GroupKey() {}
	public GroupKey(String name, String apiKeyForGet, String apiKeyForPost) {
		this.groupName = name;
		this.keyForGet = apiKeyForGet;
		this.keyForPost = apiKeyForPost;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getKeyForGet() {
		return keyForGet;
	}

	public void setKeyForGet(String keyForGet) {
		this.keyForGet = keyForGet;
	}
	public String getKeyForPost() {
		return keyForPost;
	}
	public void setKeyForPost(String keyForPost) {
		this.keyForPost = keyForPost;
	}
	
}
