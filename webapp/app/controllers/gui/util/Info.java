package controllers.gui.util;

public class Info {

	private String parameter;
	private int length;

	public Info(String encodedChart, int length) {
		this.parameter = encodedChart;
		this.length = length;
	}

	public String getParameter() {
		return parameter;
	}

	public int getLength() {
		return length;
	}
	

}
