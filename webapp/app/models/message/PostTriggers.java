package models.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

public class PostTriggers {

	@JsonProperty("triggers")
    @XmlElement(name="triggers")
	public List<PostTrigger> triggers = new ArrayList<PostTrigger>();

	public List<PostTrigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<PostTrigger> triggers) {
		this.triggers = triggers;
	}

}
