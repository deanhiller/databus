package controllers.gui;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import models.message.ChartVarMeta;

public class TimePanel extends VarWrapper {

	private static final DateTimeFormatter TO_DATE = DateTimeFormat.forPattern("MMMMM dd, yyyy");
	private static final DateTimeFormatter TO_TIME = DateTimeFormat.forPattern("HH:mm:ss");
	
	private String type;
	private String numberOfPoints;
	private DateTimeDto fromDate = new DateTimeDto();
	private DateTimeDto toDate = new DateTimeDto();
	
	public TimePanel(ChartVarMeta m) {
		super(m);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNumberOfPoints() {
		return numberOfPoints;
	}

	public void setNumberOfPoints(String numberOfPoints) {
		this.numberOfPoints = numberOfPoints;
	}

	public DateTimeDto getFromDate() {
		return fromDate;
	}

	public void setFromDate(DateTimeDto fromDate) {
		this.fromDate = fromDate;
	}

	public DateTimeDto getToDate() {
		return toDate;
	}

	public void setToDate(DateTimeDto toDate) {
		this.toDate = toDate;
	}

	public void fillWrapper(Map<String, String> variables) {
		String type = variables.get("_daterangetype");
		if(type != null) {
			convert(this.getFromDate(), variables, "_fromepoch");
			convert(this.getToDate(), variables, "_toepoch");

			String number = variables.get("_numberpoints");
			this.setNumberOfPoints(number);
			this.setType(type);
		}
	}

	private static void convert(DateTimeDto dateDto,
			Map<String, String> variables, String name) {
		String type = variables.get("chart."+name+".type");
		String epoch = variables.get(name);
		if(epoch == null)
			return;

		long longVal = Long.parseLong(epoch);
		DateTime jodaVal = new DateTime(longVal);
		String date = TO_DATE.print(jodaVal);
		String time = TO_TIME.print(jodaVal);

		dateDto.setEpochOffset(epoch);
		dateDto.setDate(date);
		dateDto.setTime(time);
		dateDto.setType(type);
	}	
}
