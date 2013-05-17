package controllers.gui;

public class Axis {

	private String name;
	private String units;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}

	public void validate(int index) {
		Chart.validate("chart.axis"+index+".name", name);
		Chart.validate("chart.axis"+index+".units", units);
	}
	
}
