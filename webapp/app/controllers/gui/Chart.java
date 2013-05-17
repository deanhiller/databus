package controllers.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.junit.Ignore;

import play.data.validation.Validation;
import play.mvc.Scope.Flash;

public class Chart {

	private String title;
	private String url;
	private String timeColumn;
	private List<Axis> axis = new ArrayList<Axis>();
	private String[] columns = new String[5];
	private long startTime;
	private long endTime;
	private static Pattern pattern;

	static {
		pattern = Pattern.compile("^[A-Za-z0-9:/s \\.]+$");
	}
	
	public Chart() {
		axis.add(new Axis());
		axis.add(new Axis());
		axis.add(new Axis());
	}

	@JsonIgnore
	public String getGeneratedJavascript() {
		String javascript = "";
		for(int i = 0; i < columns.length; i++) {
			String col = columns[i];
			if(col == null || "".equals(col.trim()))
				continue;
			
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
	public String getTimeColumn() {
		return timeColumn;
	}
	public void setTimeColumn(String timeColumn) {
		this.timeColumn = timeColumn;
	}
	public String getColumn1() {
		return columns[0];
	}
	public void setColumn1(String column1) {
		this.columns[0] = column1;
	}
	public String getColumn2() {
		return columns[1];
	}
	public void setColumn2(String column2) {
		this.columns[1] = column2;
	}
	public String getColumn3() {
		return columns[2];
	}
	public void setColumn3(String column3) {
		this.columns[2] = column3;
	}
	public String getColumn4() {
		return columns[3];
	}
	public void setColumn4(String column4) {
		this.columns[3] = column4;
	}
	public String getColumn5() {
		return columns[4];
	}
	public void setColumn5(String column5) {
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

	public boolean validate() {
		validate("chart.title", title);
		validate("chart.url", url);
		validate("chart.timeColumn", timeColumn);
		for(int i = 0; i < columns.length; i++) {
			validate("chart.column"+(i+1), columns[i]);
		}

		for(int i = 0; i < axis.size(); i++) {
			Axis a = axis.get(i);
			a.validate(i+1);
		}
		
		Validation validation = Validation.current();
		if(validation.hasErrors())
			return false;
		return true;
	}

	private boolean some() {
		return true;
	}
	public static void validate(String id, String input) {
		if(input == null || "".equals(input))
			return;

		Matcher matcher = pattern.matcher(input);
		if(matcher.matches())
			return;

		Validation validation = Validation.current();
		validation.addError(id, "This field can only contain alphanumeric characters");
	}

}
