package models.message;

import java.util.ArrayList;
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
public class ChartMeta {
	
	@JsonProperty("name")
	@XmlElement(name="name")
    public String name;
	
	@JsonProperty("description")
    @XmlElement(name="description")
    public String description;
	
	@JsonProperty("pages")
    @XmlElement(name="pages")
	private List<ChartPageMeta> pages;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ChartPageMeta> getPages() {
		return pages;
	}

	public void setPages(List<ChartPageMeta> pages) {
		this.pages = pages;
	}

}
