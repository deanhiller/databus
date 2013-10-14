package controllers.gui;

public class DateTimeDto {

	private String type;
	private String date;
	private String time;
	private String epochOffset;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getEpochOffset() {
		return epochOffset;
	}
	public void setEpochOffset(String epochOffset) {
		this.epochOffset = epochOffset;
	}
	
}
