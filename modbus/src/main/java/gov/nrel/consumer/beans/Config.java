package gov.nrel.consumer.beans;

import java.util.logging.Logger;


public class Config {
	private static final Logger logger = Logger.getLogger(Config.class.getName());


	private int scanInterval;
	private int broadcastInterval;
	private int range;
	private int numThreads;
	private String networkDevice;
	private boolean verboseLogging;
	private boolean veryVerboseLogging;
	private int deviceId;
	private String filterFileName;
	private boolean databusEnabled;
	private String databusDeviceTable;
	private String databusStreamTable;
	private String databusUserName;
	private String databusKey;
	private String databusUrl;
	private int databusPort;
	private String loggingPropertiesFileName;
	private boolean slaveDeviceEnabled;
	private String slaveDeviceConfigFile;
	private int slaveDeviceUpdateInterval;

	public Config(int scanInterval, int broadcastInterval, int range, int numThreads, String networkDevice, boolean verboseLogging,
	    boolean veryVerboseLogging, int deviceId, String filterFileName, String loggingPropertiesFileName, boolean databusEnabled,
	    String databusDeviceTable, String databusStreamTable, String databusUserName, String databusKey,
	    String databusUrl, int databusPort, boolean slaveDeviceEnabled, String slaveDeviceConfigFile, int slaveDeviceUpdateInterval) {
		this.scanInterval = scanInterval;
		this.broadcastInterval = broadcastInterval;
		this.range = range;
		this.numThreads = numThreads;
		this.networkDevice = networkDevice;
		this.verboseLogging = verboseLogging;
		this.veryVerboseLogging = veryVerboseLogging;
		this.deviceId = deviceId;
		this.filterFileName = filterFileName;
		this.loggingPropertiesFileName = loggingPropertiesFileName;
		this.databusEnabled = databusEnabled;
		this.databusDeviceTable = databusDeviceTable;
		this.databusStreamTable = databusStreamTable;
		this.databusUserName = databusUserName;
		this.databusKey = databusKey;
		this.databusUrl = databusUrl; 
		this.databusPort = databusPort;
		this.slaveDeviceEnabled = slaveDeviceEnabled;
		this.slaveDeviceConfigFile = slaveDeviceConfigFile;
		this.slaveDeviceUpdateInterval = slaveDeviceUpdateInterval;


		logger.info("Config initialized scanInterval: " + this.scanInterval);
		logger.info("Config initialized broadcastInterval: " + this.broadcastInterval);
		logger.info("Config initialized range: " + this.range);
		logger.info("Config initialized numThreads: " + this.numThreads);
		logger.info("Config initialized networkDevice: " + this.networkDevice);
		logger.info("Config initialized verboseLogging: " + this.verboseLogging);
		logger.info("Config initialized veryVerboseLogging: " + this.veryVerboseLogging);
		logger.info("Config initialized deviceId: " + this.deviceId);
		logger.info("Config initialized filterFileName: " + this.filterFileName);
		logger.info("Config initialized loggingPropertiesFileName: " + this.loggingPropertiesFileName);
		logger.info("Config initialized databusEnabled: " + this.databusEnabled);
		logger.info("Config initialized databusDeviceTable: " + this.databusDeviceTable);
		logger.info("Config initialized databusStreamTable: " + this.databusStreamTable);
		logger.info("Config initialized databusUserName: " + this.databusUserName);
		logger.info("Config initialized databusKey: " + this.databusKey);
		logger.info("Config initialized databusUrl: " + this.databusUrl);
		logger.info("Config initialized databusPort: " + this.databusPort);
		logger.info("Config initialized slaveDeviceEnabled: " + this.slaveDeviceEnabled);
		logger.info("Config initialized slaveDeviceConfigFile: " + this.slaveDeviceConfigFile);
		logger.info("Config initialized slaveDeviceUpdateInterval: " + this.slaveDeviceUpdateInterval);
	}

	public String getLoggingPropertiesFileName() {
		return loggingPropertiesFileName;
	}

	public String getNetworkDevice() {
		return networkDevice;
	}
	public int getScanInterval() {
		return scanInterval;
	}

	public int getBroadcastInterval() {
		return broadcastInterval;
	}

	public int getRange() {
		return range;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public boolean getVerboseLogging() {
		return verboseLogging;
	}

	public boolean getVeryVerboseLogging() {
		return veryVerboseLogging;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public String getFilterFileName() {
		return filterFileName;
	}

	public boolean getDatabusEnabled() {
		return databusEnabled;
	}

	public String getDatabusDeviceTable() {
		return databusDeviceTable;
	}

	public String getDatabusStreamTable() {
		return databusStreamTable;
	}

	public String getDatabusUserName() {
		return databusUserName;
	}

	public String getDatabusKey() {
		return databusKey;
	}

	public String getDatabusUrl() {
		return databusUrl;
	}

	public int getDatabusPort() {
		return databusPort;
	}

	public boolean getSlaveDeviceEnabled() {
		return slaveDeviceEnabled;
	}

	public String getSlaveDeviceConfigFile() {
		return slaveDeviceConfigFile;
	}

	public int getSlaveDeviceUpdateInterval() {
		return slaveDeviceUpdateInterval;
	}
	

//	public int deviceMatches(RemoteDevice t_rd) {
//		return oidMatches(t_rd, null);
//	}
//
//	public int oidMatches(RemoteDevice t_rd, ObjectIdentifier t_id) {
//		Vector<DeviceFilter> t_filters = getFilters();
//		if (t_filters == null) {
//			return getDefaultPollInterval();
//		}
//
//		for (DeviceFilter filter : t_filters) {
//			if (filter.matches(t_rd)) {
//				if (t_id == null) {
//					return filter.timeBetweenScans;
//				} else if (filter.oidFilters.isEmpty()) {
//					// if there's no oidfilters, accept all of them for this
//					// device
//					return filter.timeBetweenScans;
//				}
//				
//				for (OIDFilter oidFilter : filter.oidFilters) {
//					if (oidFilter.matches(t_id)) {
//						return oidFilter.timeBetweenScans;
//					}
//				}
//			}
//		}
//
//		return getDefaultPollInterval();
//	}	
}
