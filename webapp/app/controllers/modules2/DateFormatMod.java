package controllers.modules2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import play.mvc.results.BadRequest;

import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushOrPullProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class DateFormatMod extends PushOrPullProcessor {

	private String timeColumnName;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
	
	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, HashMap<String, String> options) {
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
				dateFormat = new SimpleDateFormat(dateFormatOption);
			}
			catch (IllegalArgumentException iae) {
				throw new BadRequest("The date format you specified ("+dateFormatOption+") is not a valid date format.  See this page for information on valid date formats:  http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html");
			}
		}
		
		return newPath;
	}
	
	@Override
	protected TSRelational modifyRow(TSRelational row) {
		BigInteger val = (BigInteger)row.get(timeColumnName);
		if(val != null)
			row.put(timeColumnName, formatDate(val));
		
		return row;
	}

	private String formatDate(BigInteger val) {
		return dateFormat.format(new Date(val.longValue()));
	}

}
