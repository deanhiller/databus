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
public class DatasetColumnModel {
	
	@JsonProperty("name")
    @XmlElement(name="name")
    public String name;
	
	@JsonProperty("dataType")
    @XmlElement(name="dataType")
    public String dataType;
	
	@JsonProperty("semanticType")
    @XmlElement(name="semanticType")
    public String semanticType;
	
	@JsonProperty("isIndex")
    @XmlElement(name="isIndex")
    public boolean isIndex;
	
	@JsonProperty("isPrimaryKey")
    @XmlElement(name="isPrimaryKey")
    public boolean isPrimaryKey;
	
	@JsonProperty("isFacet")
    @XmlElement(name="isFacet")
    public boolean isFacet;

	@JsonProperty("fkTableName")
    @XmlElement(name="fkTableName")
    public String foreignKeyTablename;
	
	@JsonProperty("semantics")
    @XmlElement(name="semantics")
	public List<DatasetColumnSemanticModel> semantics = new ArrayList<DatasetColumnSemanticModel>();
	
	
	// Default constructor stuff
	{
		isIndex = false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getForeignKeyTablename() {
		return foreignKeyTablename;
	}

	public void setForeignKeyTablename(String foreignKeyTablename) {
		this.foreignKeyTablename = foreignKeyTablename;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getSemanticType() {
		return semanticType;
	}

	public void setSemanticType(String semanticType) {
		this.semanticType = semanticType;
	}

	public boolean getIsIndex() {
		return isIndex;
	}

	public void setIsIndex(boolean isIndex) {
		this.isIndex = isIndex;
	}
	
	public boolean getIsFacet() {
		return isFacet;
	}

	public void setIsFacet(boolean isFacet) {
		this.isFacet = isFacet;
	}

	public List<DatasetColumnSemanticModel> getSemantics() {
		return semantics;
	}

	public void setSemantics(List<DatasetColumnSemanticModel> semantics) {
		this.semantics = semantics;
	}

	public boolean getIsPrimaryKey() {
		return isPrimaryKey;
	}

	public void setIsPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}
	
} // DatasetColumnModel

