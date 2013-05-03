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
public class ModuleHelp {
	
	@JsonProperty("modulename")
	@XmlElement(name="modulename")
	private String moduleName;
	
	@JsonProperty("description")
    @XmlElement(name="description")
	private String description;
	
	@JsonProperty("versions")
    @XmlElement(name="versions")
	private List<ModuleVersion> versions = new ArrayList<ModuleVersion>();

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ModuleVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<ModuleVersion> versions) {
		this.versions = versions;
	}
	
} // Register
