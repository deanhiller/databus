package controllers.gui;

import models.message.ChartVarMeta;

public class TimePanel extends VarWrapper {

	private String type;
	private String numberOfPoints;
	private DateTimeDto fromDate = new DateTimeDto();
	private DateTimeDto toDate = new DateTimeDto();
	
	public TimePanel(ChartVarMeta m) {
		super(m);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNumberOfPoints() {
		return numberOfPoints;
	}

	public void setNumberOfPoints(String numberOfPoints) {
		this.numberOfPoints = numberOfPoints;
	}

	public DateTimeDto getFromDate() {
		return fromDate;
	}

	public void setFromDate(DateTimeDto fromDate) {
		this.fromDate = fromDate;
	}

	public DateTimeDto getToDate() {
		return toDate;
	}

	public void setToDate(DateTimeDto toDate) {
		this.toDate = toDate;
	}
	
}
