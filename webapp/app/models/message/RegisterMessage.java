package models.message;

import java.math.BigDecimal;
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
public class RegisterMessage {
	
	@JsonProperty("datasetType")
	@XmlElement(name="datasetType")
    public DatasetType datasetType;
	
	@JsonProperty("modelName")
    @XmlElement(name="modelName")
    public String modelName;

	@JsonProperty("isForLogging")
    @XmlElement(name="isForLogging")
    public Boolean isForLogging;
	
	@JsonProperty("searchableParent")
    @XmlElement(name="searchableParent")
	private List<SearchableParent> searchableParent;
	
	@JsonProperty("isSearchable")
    @XmlElement(name="isSearchable")
	private boolean isSearchable;
	
	@JsonProperty("groups")
	@XmlElement(name="groups")
    public List<String> groups = new ArrayList<String>();
	
	@JsonProperty("tags")
	@XmlElement(name="tags")
    public List<String> tags = new ArrayList<String>();
	
	@JsonProperty("schema")
	@XmlElement(name="schema")
    public String schema;
	
	@JsonProperty("columns")
    @XmlElement(name="columns")
    public List<DatasetColumnModel> columns = new ArrayList<DatasetColumnModel>();
	
	@JsonProperty("lat")
    @XmlElement(name="lat")
	private BigDecimal lat;
	
	@JsonProperty("lon")
    @XmlElement(name="lon")
	private BigDecimal lon;
	
	
	public boolean getIsSearchable() {
		return isSearchable;
	}

	public void setIsSearchable(boolean isIndex) {
		this.isSearchable = isIndex;
	}
	
	public void setDatasetType(DatasetType type) {
		this.datasetType = type;
	} // setDatasetType

	public DatasetType getDatasetType() {
		return datasetType;
	}
	
	/**
	 * Sets the columns using either and array of strings or a variadic number
	 * of string arguments.
	 * @param columns
	 */
	public void setColumns(List<DatasetColumnModel> columns) {
		this.columns = columns;
	} // setColumns
	
	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}
	
	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<DatasetColumnModel> getColumns() {
		return columns;
	}
	
	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public List<SearchableParent> getSearchableParent() {
		return searchableParent;
	}

	public void setSearchableParent(List<SearchableParent> searchableParent) {
		this.searchableParent = searchableParent;
	}

	public Boolean getIsForLogging() {
		return isForLogging;
	}

	public void setIsForLogging(Boolean isForLogging) {
		this.isForLogging = isForLogging;
	}

	public BigDecimal getLat() {
		return lat;
	}

	public void setLat(BigDecimal lat) {
		this.lat = lat;
	}

	public BigDecimal getLon() {
		return lon;
	}

	public void setLon(BigDecimal lon) {
		this.lon = lon;
	}

} // Register
