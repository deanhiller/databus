package controllers.gui;

import models.message.ChartVarMeta;

public class VarWrapper {

	private ChartVarMeta meta;
	private String value;
	private String timeValue;
	private String dateValue;
	
	public VarWrapper(ChartVarMeta m) {
		this.meta = m;
	}
	
	public VarWrapper(ChartVarMeta m, String v) {
		this.meta = m;
		this.value = v;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ChartVarMeta getMeta() {
		return meta;
	}

	public String getDateValue() {
		return dateValue;
	}

	public void setDateValue(String dateValue) {
		this.dateValue = dateValue;
	}

	public String getTimeValue() {
		return timeValue;
	}

	public void setTimeValue(String timeValue) {
		this.timeValue = timeValue;
	}

}
