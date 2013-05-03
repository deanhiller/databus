package models.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This is a registration message
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ModuleVersion {
	
	@JsonProperty("version")
	@XmlElement(name="version")
    public int version;
	
	@JsonProperty("technical")
    @XmlElement(name="technical")
    public String technical;
	
	@JsonProperty("non-technical")
    @XmlElement(name="non-technical")
	private String nonTechnical;

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getTechnical() {
		return technical;
	}

	public void setTechnical(String technical) {
		this.technical = technical;
	}

	public String getNonTechnical() {
		return nonTechnical;
	}

	public void setNonTechnical(String nonTechnical) {
		this.nonTechnical = nonTechnical;
	}


} // Register
