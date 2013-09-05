package models.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PostTrigger {
	
	@JsonProperty("database")
	@XmlElement(name="database")
    public String database;

	@JsonProperty("table")
	@XmlElement(name="table")
    public String table;

	@JsonProperty("scriptLanguage")
    @XmlElement(name="scriptLanguage")
    public String scriptLanguage;
	
	@JsonProperty("script")
    @XmlElement(name="script")
    public String script;

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getScriptLanguage() {
		return scriptLanguage;
	}

	public void setScriptLanguage(String scriptLanguage) {
		this.scriptLanguage = scriptLanguage;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public static PostTrigger transform(DboTableMeta table, String database) {
		Map<String, String> extensions = table.getExtensions();
		String lang = extensions.get("databus.lang");
		String script = extensions.get("databus.script");
		PostTrigger t = new PostTrigger();
		t.setDatabase(database);
		t.setTable(table.getColumnFamily());
		t.setScript(script);
		t.setScriptLanguage(lang);
		return t;
	}

} // Register
