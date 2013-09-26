package controllers.gui;

import models.message.ChartVarMeta;

public class VarWrapper {

	private ChartVarMeta meta;
	private String value;

	public VarWrapper(ChartVarMeta m) {
		this.meta = m;
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

}
