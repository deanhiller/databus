package models.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonProperty;

public class Triggers {

	@JsonProperty("triggers")
    @XmlElement(name="triggers")
	public List<Trigger> triggers = new ArrayList<Trigger>();

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<Trigger> triggers) {
		this.triggers = triggers;
	}

}
