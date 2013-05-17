package controllers.gui;

import org.codehaus.jackson.annotate.JsonIgnore;

import play.templates.JavaExtensions;

public class ChartForJavascript {

	private Chart chart;

	public ChartForJavascript(Chart theChart) {
		this.chart = theChart;
	}
	@JsonIgnore
	public String getGeneratedJavascript() {
		String javascript = "";
		
		String[] columns = chart.getColumns();
		for(int i = 0; i < columns.length; i++) {
			String theCol = columns[i];
			if(theCol == null || "".equals(theCol.trim()))
				continue;
			
			String col = JavaExtensions.escapeJavaScript(theCol);
			javascript += "{ name: '"+col+"', data: collapseData(data, '"+col+"') }";
			if(i < columns.length-1)
				javascript += ",";
//		{
//            name: 'Aggregation',
//            data: collapseData(data, (singleStream!==true ? true : false))
//          }
		}

		return javascript;
	}

	public String getTitle() {
		return JavaExtensions.escapeJavaScript(chart.getTitle());
	}

	public String getUrl() {
		String url = JavaExtensions.escapeJavaScript(chart.getUrl());
		return url.replace("\\/", "/");
	}

	public String getTimeColumn() {
		return JavaExtensions.escapeJavaScript(chart.getTimeColumn());
	}

	public long getStartTime() {
		return chart.getStartTime();
	}
	
	public long getEndTime() {
		return chart.getEndTime();
	}

	public String getColumn1() {
		return JavaExtensions.escapeJavaScript(chart.getColumn1());
	}
}
