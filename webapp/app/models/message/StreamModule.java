package models.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import models.SecureTable;

import org.codehaus.jackson.annotate.JsonProperty;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StreamModule {

	@JsonProperty("module")
    @XmlElement(name="module")
    public String module;

	@JsonProperty("params")
    @XmlElement(name="params")
    public Map<String, String> params = new HashMap<String, String>();

	//I want to do the composite pattern and push this field down into the "Container" while StreamModule is the "Component" of that pattern
	//but that would not work as unmarshalling the json would break since parser does not know to parse to StreamModule or the Container type which
	//I had previously called StreamAggregation so this field is only used for the Container type
	@JsonProperty("childStreams")
	@XmlElement(name="childStreams")
    public List<StreamModule> streams = new ArrayList<StreamModule>();

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public List<StreamModule> getStreams() {
		return streams;
	}

	public void setStreams(List<StreamModule> streams) {
		this.streams = streams;
	}

	@Override
	public String toString() {
		return "module=" + module;
	}

} // Register
