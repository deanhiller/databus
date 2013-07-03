package models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptChart {
	String fileName;
	String chartName;
	String title;
	String url;
	models.UserChart.ChartType chartType = models.UserChart.ChartType.SCRIPT;
	models.UserChart.ChartLibrary chartLibrary = models.UserChart.ChartLibrary.HIGHCHARTS;
	
	public ScriptChart(String chart, String fileName) {
		this.fileName = fileName;
		
		/**
		 * |DATABUS_CHART|
		 * ***********************************************************************
		 * REQUIRED FOR DATABUS URI  -- START --
		 * ***********************************************************************
		 * 
		 * The following information MUST be present in the chart code or the
		 * Databus chart loader might encounter errors.
		 *//*
		var _name = '3phaseRealPower (RANGED)';
		var _title = '3phaseRealPower D73937773/D73937763 +-2000';
		 
		var _protocol = 'http';
		var _database = '/api/firstvaluesV1/50/aggregation/RSF_PV_1MIN?reverse=true';
		*//**
		 * |DATABUS_CHART|
		 * ***********************************************************************
		 * REQUIRED FOR DATABUS URI  -- END --
		 * ***********************************************************************
		 */
		Pattern _nameP = Pattern.compile("var _name = '(.*)';");
		Matcher _nameM = _nameP.matcher(chart);
		if (_nameM.find()) {
		    this.chartName = _nameM.group(1);
		}
		
		Pattern _titleP = Pattern.compile("var _title = '(.*)';");
		Matcher _titleM = _titleP.matcher(chart);
		if (_titleM.find()) {
		    this.title = _titleM.group(1);
		}
		
		Pattern _databaseP = Pattern.compile("var _database = '(.*)';");
		Matcher _databaseM = _databaseP.matcher(chart);
		if (_databaseM.find()) {
		    this.url = _databaseM.group(1);
		}
	}
	
	public ScriptChart(String chartName, String fileName, String title, String url) {
		this.chartName = chartName;
		this.fileName = fileName;
		this.title = title;
		this.url = url;
	}
	
	public boolean validate() {
		if((this.fileName == null) || (this.fileName.equals(""))) {
			return false;
		}
		
		if((this.chartName == null) || (this.chartName.equals(""))) {
			return false;
		}
		
		if((this.title == null) || (this.title.equals(""))) {
			return false;
		}
		
		if((this.url == null) || (this.url.equals(""))) {
			return false;
		}
		
		return true;
	}

	public String getChartName() {
		return chartName;
	}

	public void setChartName(String chartName) {
		this.chartName = chartName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public models.UserChart.ChartType getChartType() {
		return chartType;
	}

	public void setChartType(models.UserChart.ChartType chartType) {
		this.chartType = chartType;
	}

	public models.UserChart.ChartLibrary getChartLibrary() {
		return chartLibrary;
	}

	public void setChartLibrary(models.UserChart.ChartLibrary chartLibrary) {
		this.chartLibrary = chartLibrary;
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append("\nChart Name:\t" + this.chartName);
		result.append("\nChart Title:\t" + this.title);
		result.append("\nChart URL:\t" + this.url);
		result.append("\nChart FILE:\t" + this.fileName);
		
		return result.toString();
	}
}
