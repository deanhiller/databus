package controllers.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.junit.Ignore;

import play.data.validation.Validation;
import play.mvc.Scope.Flash;
import play.templates.JavaExtensions;
import play.utils.HTML;

public class Chart {

	private String title;
	private String url;
	private String timeColumn;
	private List<Axis> axis = new ArrayList<Axis>();
	private ChartSeries[] columns = new ChartSeries[5];
	private long startTime;
	private long endTime;
	private static Pattern pattern;

	static {
		pattern = Pattern.compile("^[A-Za-z0-9:/s \\.]+$");
	}
	
	public Chart() {
		Axis a1 = new Axis();
		a1.setOpposite(false);
		Axis a2 = new Axis();
		a2.setOpposite(true);
		Axis a3 = new Axis();
		a3.setOpposite(false);
		
		a1.setColor("89A54E");
		a2.setColor("4572A7");
		a3.setColor("AA4643");
		
		axis.add(a1);
		axis.add(a2);
		axis.add(a3);
	}

	public String getTitle() {
		return JavaExtensions.escapeJavaScript(title);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		String url = JavaExtensions.escapeJavaScript(this.url);
		return url.replace("\\/", "/");
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTimeColumn() {
		return timeColumn;
	}
	public void setTimeColumn(String timeColumn) {
		this.timeColumn = timeColumn;
	}
	public ChartSeries getSeries1() {
		return columns[0];
	}
	public void setSeries1(ChartSeries column1) {
		this.columns[0] = column1;
	}
	public ChartSeries getSeries2() {
		return columns[1];
	}
	public void setSeries2(ChartSeries column2) {
		this.columns[1] = column2;
	}
	public ChartSeries getSeries3() {
		return columns[2];
	}
	public void setSeries3(ChartSeries column3) {
		this.columns[2] = column3;
	}
	public ChartSeries getSeries4() {
		return columns[3];
	}
	public void setSeries4(ChartSeries column4) {
		this.columns[3] = column4;
	}
	public ChartSeries getSeries5() {
		return columns[4];
	}
	public void setSeries5(ChartSeries column5) {
		this.columns[4] = column5;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
	
	public Axis getAxis1() {
		return axis.get(0);
	}
	
	public void setAxis1(Axis a) {
		axis.set(0, a);
	}
	
	public Axis getAxis2() {
		return axis.get(1);
	}
	public void setAxis2(Axis a) {
		axis.set(1, a);
	}
	public Axis getAxis3() {
		return axis.get(2);
	}
	public void setAxis3(Axis a) {
		axis.set(2, a);
	}

	@JsonIgnore
	public List<ChartSeries> getSeriesList() {
		List<ChartSeries> result = new ArrayList<ChartSeries>();
		for(ChartSeries s : columns) {
			if(!StringUtils.isEmpty(s.getName())) 
				result.add(s);
		}
		return result;
	}
	
	@JsonIgnore
	public List<Axis> getAxisList() {
		List<Axis> axisList = this.axis;
		for(int i = axisList.size()-1; i >= 0; i--) {
			Axis axis = axisList.get(i);
			if(StringUtils.isEmpty(axis.getName()))
				axisList.remove(axis);
		}
		return axisList;
	}

	@JsonIgnore
	public String getGeneratedJavascript() {
		String javascript = "";
		
		
		for(int i = 0; i < columns.length; i++) {
			ChartSeries series = columns[i];
			String theCol = series.getName();
			if(theCol == null || "".equals(theCol.trim()))
				continue;
			if(i > 0)
				javascript += ",";
			
			String col = JavaExtensions.escapeJavaScript(theCol);
			javascript += "{ name: '"+col+"', data: collapseData(data, '"+col+"') }";

//		{
//            name: 'Aggregation',
//            data: collapseData(data, (singleStream!==true ? true : false))
//          }
		}

		return javascript;
	}

	public void fillIn() {
		boolean opp = false;
		for(Axis a : axis) {
			if(a != null) {
				a.setOpposite(opp);
				opp = !opp;
			}
		}
		
		fillSeriesColor(0, "#89A54E");
		fillSeriesColor(1, "#4572A7");
		fillSeriesColor(2, "#AA4643");
		fillSeriesColor(3, "#000000");
		fillSeriesColor(4, "#FF0000");
	}

	private void fillSeriesColor(int i, String color) {
		ChartSeries s = columns[i];
		if(StringUtils.isEmpty(s.getColor())) {
			s.setColor(color);
		}		
	}

	public void fillInAxisColors() {
		fillColor(0, "#89A54E");
		fillColor(1, "#4572A7");
		fillColor(2, "#AA4643");
	}

	private void fillColor(int index, String color) {
		Axis axis1 = axis.get(index);
		if(StringUtils.isEmpty(axis1.getColor()))
			axis1.setColor(color);
	}
}
