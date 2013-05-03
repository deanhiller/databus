package models.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This is a registration message
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchableParent {
	
	@JsonProperty("parentTable")
	@XmlElement(name="parentTable")
	public String parentTable;
	
	@JsonProperty("childFKField")
	@XmlElement(name="childFKField")
    public String childFKField;
	
	@JsonProperty("parentFKField")
	@XmlElement(name="parentFKField")
    public String parentFKField;
	

	
	public String getParentTable() {
		return parentTable;
	}

	public void setParentTable(String parentTable) {
		this.parentTable = parentTable;
	}

	public String getChildFKField() {
		return childFKField;
	}

	public void setChildFKField(String childFKField) {
		this.childFKField = childFKField;
	}

	public String getParentFKField() {
		return parentFKField;
	}

	public void setParentFKField(String parentFKField) {
		this.parentFKField = parentFKField;
	}

}
