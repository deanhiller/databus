package models.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import models.SecureTable;

import org.codehaus.jackson.annotate.JsonProperty;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StreamEditor {
	
	@JsonProperty("stream")
	@XmlElement(name="stream")
    public StreamModule stream = new StreamModule();

	@JsonProperty("location")
    @XmlElement(name="location")
    public List<Integer> location = new ArrayList<Integer>();

	public StreamModule getStream() {
		return stream;
	}

	public void setStream(StreamModule stream) {
		this.stream = stream;
	}

	public List<Integer> getLocation() {
		return location;
	}

	public void setLocation(List<Integer> location) {
		this.location = location;
	}

} // Register
