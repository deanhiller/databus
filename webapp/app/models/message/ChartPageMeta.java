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
public class ChartPageMeta {
	
	@JsonProperty("title")
    @XmlElement(name="title")
    public String title;
	
	@JsonProperty("variables")
    @XmlElement(name="variables")
    public List<ChartVarMeta> variables;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ChartVarMeta> getVariables() {
		return variables;
	}

	public void setVariables(List<ChartVarMeta> variables) {
		this.variables = variables;
	}

}
