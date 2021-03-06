package controllers.gui.util;

import java.util.Map;

import play.templates.JavaExtensions;
import play.utils.HTML;
import models.ChartDbo;
import models.message.ChartMeta;

public class ChartInfo {

	public static final String BUILT_IN_CHART1 = "databus-built-in1";
	private ChartMeta chartMeta;
	private String htmlHeaders;
	private String largeChart;
	private String smallChart;

	private long lastModified;
	private String route;
	private int chartVersion;
	private String id;
	
	public ChartInfo(String id, String route, String name, String description) {
		this.id = id;
		this.route = route;
		chartMeta = new ChartMeta();
		chartMeta.setDescription(description);
		chartMeta.setName(name);
	}
	
	public ChartInfo() {
	}

	public String getName() {
		return HTML.htmlEscape(chartMeta.getName());
	}

	public String getHtmlHeaders() {
		return htmlHeaders;
	}

	public void setHtmlHeaders(String htmlHeaders) {
		this.htmlHeaders = htmlHeaders;
	}

	public String getLargeChart() {
		return largeChart;
	}

	public void setLargeChart(String largeChart) {
		this.largeChart = largeChart;
	}

	public String getSmallChart() {
		if(smallChart == null)
			return largeChart;
		return smallChart;
	}

	public void setSmallChart(String smallChart) {
		this.smallChart = smallChart;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}
	
	public boolean isBuiltinChart() {
		return route != null;
	}

	public ChartMeta getChartMeta() {
		return chartMeta;
	}

	public void setChartMeta(ChartMeta chartMeta) {
		this.chartMeta = chartMeta;
	}

	public void setChartVersion(int chartVersion) {
		this.chartVersion = chartVersion;
	}

	public int getChartVersion() {
		return chartVersion;
	}

	public void setAndConvertId(String id) {
		this.id = convert(id);
	}
	public static String convert(String name) {
		name = name.replaceAll("\\.", "-");
		return name;
	}

	public String getId() {
		return id;
	}

}
