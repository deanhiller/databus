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
public class AllModuleHelp {
	
	@JsonProperty("modules")
	@XmlElement(name="modules")
	private List<ModuleHelp> modules = new ArrayList<ModuleHelp>();

	public List<ModuleHelp> getModules() {
		return modules;
	}

	public void setModules(List<ModuleHelp> modules) {
		this.modules = modules;
	}
	
} // Register
