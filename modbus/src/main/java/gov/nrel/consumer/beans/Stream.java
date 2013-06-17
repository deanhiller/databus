package gov.nrel.consumer.beans;

public class Stream {

	private String tableName;
	
	private String device;
	private String streamId;
	private String streamType;
	private String streamDescription;
	private String units;
	private String virtual = "false";
	private String aggInterval = "raw";
	private String aggType = "raw";
	private String processed = "raw";
	
	public String getStreamId() {
		return streamId;
	}
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	public String getStreamType() {
		return streamType;
	}
	public void setStreamType(String streamType) {
		this.streamType = streamType;
	}
	public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getStreamDescription() {
		return streamDescription;
	}
	public void setStreamDescription(String streamDescription) {
		this.streamDescription = streamDescription;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public String getVirtual() {
		return virtual;
	}
	public void setVirtual(String virtual) {
		this.virtual = virtual;
	}
	public String getAggInterval() {
		return aggInterval;
	}
	public void setAggInterval(String aggInterval) {
		this.aggInterval = aggInterval;
	}
	public String getAggType() {
		return aggType;
	}
	public void setAggType(String aggType) {
		this.aggType = aggType;
	}
	public String getProcessed() {
		return processed;
	}
	public void setProcessed(String processed) {
		this.processed = processed;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Stream other = (Stream) obj;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}

}
