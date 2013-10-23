package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.mortbay.log.Log;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class DateFormatMod extends PushOrPullProcessor {

	private String timeColumnName;
	private String dateFormatString = "yyyyMMdd'T'HHmmss'Z'";
	private String timeZone = null;
	private SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
	private boolean reversed = false;
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(pathStr, nextInChain, visitor, options);
		//default to the superclasses timecolumn
		timeColumnName = timeColumn;
		//only if overridden in the options use that col name instead
		String timeColNameOption = options.get("columnName");
		if (StringUtils.isNotBlank(timeColNameOption))
			timeColumnName = timeColNameOption;
		
		String dateFormatOption = options.get("dateFormat");
		if (StringUtils.isNotBlank(dateFormatOption)) {
			try {
				dateFormatString = dateFormatOption;
				dateFormat = new SimpleDateFormat(dateFormatOption);
			}
			catch (IllegalArgumentException iae) {
				throw new BadRequest("The date format you specified ("+dateFormatOption+") is not a valid date format.  See this page for information on valid date formats:  http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html");
			}
		}
		
		String timeZoneOption = options.get("timeZone");
		if (StringUtils.isNotBlank(timeZoneOption))
			timeZone = timeZoneOption;
		
		String val = Request.current().params.get("reverse");
		if("true".equalsIgnoreCase(val)) {
			reversed = true;
		}
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational row) {
		BigInteger val = (BigInteger)row.get(timeColumnName);

		if(val != null)
			row.put(timeColumnName, formatDate(reversed?val.negate():val));
		
		return row;
	}

	private String formatDate(BigInteger val) {
		TimeZone tz = TimeZone.getTimeZone("GMT");
		Date dateValue = new Date(val.longValue());
		Calendar calValue = Calendar.getInstance(tz);
		//Calendar calValue = Calendar.getInstance();
		calValue.setTime(dateValue);
		
		if (dateFormatString.endsWith("'Z'"))
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (StringUtils.isNotBlank(timeZone))
			dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return dateFormat.format(new Date(val.longValue()));
	}

}
