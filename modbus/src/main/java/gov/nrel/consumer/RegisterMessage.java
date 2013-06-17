package gov.nrel.consumer;

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
	
	@JsonProperty("isSearchable")
    @XmlElement(name="isSearchable")
	private boolean isSearchable;
	
	@JsonProperty("groups")
	@XmlElement(name="groups")
    public List<String> groups = new ArrayList<String>();
	
	@JsonProperty("columns")
    @XmlElement(name="columns")
    public List<DatasetColumnModel> columns = new ArrayList<DatasetColumnModel>();
	
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

	public List<DatasetColumnModel> getColumns() {
		return columns;
	}
	
} // Register
