package gov.nrel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import models.AppProperty;

public abstract class UpgradeAbstract {

	private static final Logger log = LoggerFactory.getLogger(UpgradeAbstract.class.getName());
	
	private int version;

	public UpgradeAbstract(int currentVersion) {
		this.version = currentVersion;
	}
	
	public void readOnlyTest() {
		upgradeImpl();
	}
	
	public void upgrade() {
		if (log.isInfoEnabled())
			log.info("running upgrade checks.  checking for version="+version);
		String verStr = version+"";
		AppProperty property = NoSql.em().find(AppProperty.class, "schema.version");
		String newVersion = (version+1)+"";
		String inProgressVersion = "start"+newVersion;
		if(property == null) {
			if (log.isWarnEnabled())
				log.warn("database schema.version property found, not running upgrade as we can't figure out the version");
			return;
		} else if(!verStr.equals(property.getValue()) && !inProgressVersion.equals(property.getValue())) {
			if (log.isInfoEnabled())
				log.info("Schema version currently="+property.getValue()+" so not running upgrade.  Schema version needs to be="+version);
			return;
		}
		
		property = new AppProperty();
		property.setName("schema.version");
		property.setValue(inProgressVersion);
		NoSql.em().put(property);
		NoSql.em().flush();
		
		if (log.isInfoEnabled())
			log.info("UPGRADE STARTING since version matches="+version);
		
		NoSql.em().flush();
		
		if (log.isInfoEnabled())
			log.info("upgrade schemas");
		upgradeImpl();
		
		//put the final schema version in the database now...
		property.setValue(newVersion);
		NoSql.em().put(property);
		NoSql.em().flush();
	}

	protected abstract void upgradeImpl();
}
