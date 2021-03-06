package controllers.gui.util;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import play.templates.JavaExtensions;

public class ChartSeries {

	private String name;
	private String units;
	private String color;
	private int axisIndex;
	private String dashStyle;
	
	public String getName() {
		return JavaExtensions.escapeJavaScript(name);
	}
	public void setName(String columnName) {
		this.name = columnName;
	}
	public String getUnits() {
		return JavaExtensions.escapeJavaScript(units);
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public String getColor() {
		return JavaExtensions.escapeJavaScript(color);
	}
	public void setColor(String color) {
		if (!StringUtils.isBlank(color) && !StringUtils.startsWith(color,"#"))
			color = "#"+color;
		this.color = color;
	}
	public int getAxisIndex() {
		return axisIndex;
	}
	public void setAxisIndex(int axisIndex) {
		this.axisIndex = axisIndex;
	}
	@JsonIgnore
	public int getAxis() {
		return axisIndex-1;
	}
	
	public void setDashStyle(String style) {
		this.dashStyle = style;
	}
	public String getDashStyle() {
		return JavaExtensions.escapeJavaScript(dashStyle);
	}
}
