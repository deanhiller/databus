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
	//default for MyDataStreams stuff NOT default for MyGenericCharts which is a different type system(widget type really)
    public String type = "java.lang.String";

	@JsonProperty("defaultValue")
    @XmlElement(name="defaultValue")
    public String defaultValue;

	@JsonProperty("isRequired")
    @XmlElement(name="isRequired")
	public boolean isRequired = false;

	private transient Class<?> clazzType;

	public ChartVarMeta copy() {
		ChartVarMeta copy = new ChartVarMeta();
		copy.setDefaultValue(this.getDefaultValue());
		copy.setLabel(this.getLabel());
		copy.setNameInJavascript(this.getNameInJavascript());
		copy.setHelp(this.getHelp());
		copy.setType(this.getType());
		return copy;
	}

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

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public Class<?> getClazzType() {
		return clazzType;
	}

	public void setClazzType(Class<?> clazzType) {
		this.clazzType = clazzType;
	}

	public boolean isString() {
		if("string".equals(type.toLowerCase()))
			return true;
		return false;
	}
	
	public boolean isColumnSelector() {
		if("column".equals(type.toLowerCase()))
			return true;
		return false;
	}
	
	public boolean isDateTime() {
		if("datetime".equals(type.toLowerCase()))
			return true;
		return false;
	}
}

