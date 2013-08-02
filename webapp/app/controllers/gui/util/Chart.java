package controllers.gui.util;

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
	private static List<String> dashStyles;
	private static List<String> standardColors;

	static {
		pattern = Pattern.compile("^[A-Za-z0-9:/s \\.]+$");
		dashStyles = new ArrayList<String>();
		dashStyles.add("Solid");
		dashStyles.add("Dot");
		dashStyles.add("LongDash");
		dashStyles.add("LongDashDotDot");
		dashStyles.add("ShortDash");
		dashStyles.add("ShortDot");
		dashStyles.add("ShortDashDot");
		dashStyles.add("ShortDashDotDot");
		dashStyles.add("Dash");
		dashStyles.add("DashDot");
		dashStyles.add("LongDashDot");
		standardColors = new ArrayList<String>();
		standardColors.add("#89A54E");
		standardColors.add("#4572A7");
		standardColors.add("#AA4643");
		standardColors.add("#FFFFFF");
		standardColors.add("#FF00FF");
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
		List<Axis> axisList = new ArrayList<Axis>(this.axis);
		for(int i = axisList.size()-1; i >= 0; i--) {
			Axis axis = axisList.get(i);
			if(StringUtils.isEmpty(axis.getName()))
				axisList.remove(axis);
		}
		return axisList;
	}

	public void fillIn() {
		boolean opp = false;
		for(int i = 0; i < axis.size(); i++) {
			Axis a = axis.get(i);
			if(a != null) {
				a.setOpposite(opp);
				opp = !opp;
				if(i >= 2)
					a.setOffset(200);
				else
					a.setOffset(100);
			}
		}
	
		if(getAxisList().size() > 1) {
			fillInfo();
		} else {
			for(int i = 0;  i < columns.length; i++) {
				ChartSeries s = columns[i];
				if(StringUtils.isEmpty(s.getColor()))
					s.setColor(standardColors.get(i));
			}
		}
	}

	private void fillInfo() {
		List<ChartSeries> axis1 = new ArrayList<ChartSeries>();
		List<ChartSeries> axis2 = new ArrayList<ChartSeries>();
		List<ChartSeries> axis3 = new ArrayList<ChartSeries>();
		for(int i = 0;  i < columns.length; i++) {
			ChartSeries s = columns[i];
			if(s.getAxis() == 0)
				axis1.add(s);
			else if(s.getAxis() == 1)
				axis2.add(s);
			else
				axis3.add(s);
			int index = s.getAxis();
			Axis axis = this.axis.get(index);
			if(StringUtils.isEmpty(s.getColor()))
				s.setColor(axis.getColor());
		} 
		
		fillStyles(axis1);
		fillStyles(axis2);
		fillStyles(axis3);
	}

	private void fillStyles(List<ChartSeries> axis) {
		for(int i = 0; i < axis.size(); i++) {
			ChartSeries s = axis.get(i);
			s.setDashStyle(dashStyles.get(i));
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
