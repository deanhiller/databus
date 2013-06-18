package gov.nrel.consumer.beans;

public class DatabusBean {

	private String tableName;
	private long time;
	private Double value;
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "table="+tableName+",t="+time+",v="+value;
	}
	
}
