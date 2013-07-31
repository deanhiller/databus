package models.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ChartVarMeta {
	
	@JsonProperty("nameInJavascript")
    @XmlElement(name="nameInJavascript")
    public String nameInJavascript;
	
	@JsonProperty("label")
    @XmlElement(name="label")
    public String label;
	
	@JsonProperty("help")
    @XmlElement(name="help")
    public String help;
	
	@JsonProperty("type")
    @XmlElement(name="type")
    public String type;

	public String value;

	public String getNameInJavascript() {
		return nameInJavascript;
	}

	public void setNameInJavascript(String nameInJavascript) {
		this.nameInJavascript = nameInJavascript;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}

