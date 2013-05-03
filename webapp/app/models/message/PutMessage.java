package models.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This a PUT message
 * @author mcottere
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PutMessage {

	@JsonProperty("userName")
	@XmlElement(name="userName")
    public String userName;
	
} // PutMessage
