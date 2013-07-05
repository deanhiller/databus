package controllers.gui;

import gov.nrel.util.Utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import controllers.gui.auth.GuiSecure;
import de.undercouch.bson4jackson.BsonFactory;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);
	private static int CURRENT_VERSION = 1;
	
	public static void loadChartPage() {
		String protocol = Utility.getRedirectProtocol();
		renderArgs.put("protocol", protocol);
		
		String theRequestedChart = params.get("chart");
		
		/**
		 * Load the chart code directly into a string and render it
		 */
		String chartName = "/public/Charts/HighCharts/" + theRequestedChart;
		String theChart = play.vfs.VirtualFile.fromRelativePath(chartName).contentAsString();
		
		//log.error(theChart);
		
		renderArgs.put("theRequestedChart", theChart);
		render();
	}
	
	public static void loadChartDiv() {
		String theRequestedChart = params.get("chart");
		String theDivContainer = params.get("div");
		String overrideName = params.get("override_name");
		String overrideTitle = params.get("override_title");
		String overrideDB = params.get("override_db");
		
		log.error("\nPARAMS:\n\tCHART: " + theRequestedChart + "\n\tDIV: " + theDivContainer + "\n\tNAME: " + overrideName + "\n\tTITLE: " + overrideTitle + "\n\tDB: " + overrideDB);
		
		/**
		 * Load the chart code directly into a string and render it
		 */
		if(theRequestedChart.equals("BLANK")) {
			theRequestedChart = "blankchart.js";
		}
		String chartName = "/public/Charts/HighCharts/" + theRequestedChart;
		String theChart = play.vfs.VirtualFile.fromRelativePath(chartName).contentAsString();
		
		theChart = theChart.replace("renderTo: 'container'", "renderTo: '" + theDivContainer + "'");
		
		/**
		 * Now replace the embedded meta data
		 * 
		 * 
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
		if((overrideName != null) && (!overrideName.equals(""))) {
			Pattern _nameP = Pattern.compile("var _name = '(.*)';");
			Matcher _nameM = _nameP.matcher(theChart);
			if (_nameM.find()) {
				/**
				 * We have to replace the full pattern because its possible that the
				 * name is reused in other poritions of the script (i.e. the stream name)
				 */
			    String defaultName = "var _name = '" + _nameM.group(1) + "';";
			    String override = "var _name = '" + overrideName + "';   // replaced by controller";
			    theChart = theChart.replace(defaultName, override);
			}
		}
		
		if((overrideTitle != null) && (!overrideTitle.equals(""))) {
			Pattern _titleP = Pattern.compile("var _title = '(.*)';");
			Matcher _titleM = _titleP.matcher(theChart);
			if (_titleM.find()) {
				/**
				 * We have to replace the full pattern because its possible that the
				 * title is reused in other poritions of the script (i.e. the stream name)
				 */
			    String defaultTitle = "var _title = '" + _titleM.group(1) + "';";
			    String override = "var _title = '" + overrideTitle + "';   // replaced by controller";
			    theChart = theChart.replace(defaultTitle, override);
			}
		}
		
		if((overrideDB != null) && (!overrideDB.equals(""))) {
			Pattern _databaseP = Pattern.compile("var _database = '(.*)';");
			Matcher _databaseM = _databaseP.matcher(theChart);
			if (_databaseM.find()) {
			    String defaultDB = "var _database = '" + _databaseM.group(1) + "';";
			    String override = "var _database = '" + overrideDB + "';   // replaced by controller";
			    theChart = theChart.replace(defaultDB, override);
			}
		}

		renderText(theChart);
	}

	public static void createChart() {
		render();
	}

	public static void modifyChart(String encodedChart, int version, int length) {
		Chart chart = deserialize(encodedChart, version, length);
		render("@createChart", chart);
	}

	public static void postStep1(Chart chart) throws UnsupportedEncodingException {
		String url = chart.getUrl();
		int index = url.lastIndexOf("/");
		
		if(index < 0) {
		} else {
			String endTimeStr = url.substring(index+1);
			String left = url.substring(0, index);
			int startIndex = left.lastIndexOf("/");
			String startTimeStr = left.substring(startIndex+1);
	
			Long endTime = convertLong(endTimeStr);
			Long startTime = convertLong(startTimeStr);
			if(endTime != null && startTime != null) {
				chart.setStartTime(startTime);
				chart.setEndTime(endTime);
			}
		}

		if(chart.getTitle() == null || "".equals(chart.getTitle().trim()))
			validation.addError("chart.title", "This is a required field");

		if("".equals(chart.getAxis1().getName()))
			validation.addError("chart.axis1.name", "Name is a required field");
				
		chart.fillInAxisColors();
		Info info = createUrl(chart, 1);

		String encodedChart = info.getParameter();
		int length = info.getLength();
		createStep2(encodedChart, CURRENT_VERSION, length);
	}
	
	public static void createStep2(String encodedChart, int version, int length) {
		Chart chart = deserialize(encodedChart, version, length);
		render(chart);
	}

	public static void postStep2(Chart chart) throws UnsupportedEncodingException {
		chart.fillIn();
		Info info = createUrl(chart, 2);
		String encodedChart = info.getParameter();
		int length = info.getLength();
		drawChart(encodedChart, CURRENT_VERSION, length);
	}
	
	public static void drawChart(String encodedChart, int version, int length) {
		Chart chart = deserialize(encodedChart, version, length);
		int height = 0;
		render(chart, encodedChart, version, length, height);
	}
	
	public static void drawJustChart(String encodedChart, String title, int version, int length) {
		Chart chart = deserialize(encodedChart, version, length);
		int height = 0;
		String heightStr = params.get("height");
		if(heightStr != null) {
			String heightEsc = JavaExtensions.escapeJavaScript(heightStr);
			height = Integer.parseInt(heightEsc);
		}
		render(chart, encodedChart, version, length, height);
	}
	
	public static void justJavascript(String encodedChart, String title, int version, int length) {
		Chart chart = deserialize(encodedChart, version, length);
		
		int height = 0;
		String heightStr = params.get("height");
		if(heightStr != null) {
			String heightEsc = JavaExtensions.escapeJavaScript(heightStr);
			height = Integer.parseInt(heightEsc);
		}
		
		String divName = "mychart";
		String divNameStr = params.get("divname");
		if(divNameStr != null) {
			divName = divNameStr;
		}
		
		render(chart, encodedChart, version, length, height, divName);
	}

	private static Info createUrl(Chart chart, int stepNumber) {
		byte[] input = convert(chart);

		// Compress the bytes
		byte[] output1 = new byte[input.length];
		Deflater compresser = new Deflater();
		compresser.setInput(input);
		compresser.finish();
		int compressedDataLength = compresser.deflate(output1);

		byte[] compressed2 = Arrays.copyOfRange(output1, 0, compressedDataLength);
		String encodedChart = Base64.encodeBase64URLSafeString(compressed2);
		if(log.isInfoEnabled())
			log.info("length="+encodedChart.length()+" result="+encodedChart);
		
		int length = input.length;
		
		if(validation.hasErrors()) {
			flash.error("You have errors in your form below");
			validation.keep();
			flash.keep();
			params.flash();
			if(stepNumber == 1)
				createChart();
			else
				createStep2(encodedChart, CURRENT_VERSION, length);
		}
		
		return new Info(encodedChart, length);
	}

	private static Long convertLong(String str) {
		try {
			return Long.parseLong(str);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	private static byte[] convert(Chart chart) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectMapper mapper = new ObjectMapper(new BsonFactory());
			mapper.writeValue(baos, chart);
			return baos.toByteArray();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Chart deserialize(String encodedChart, int version,
			int length) {
		byte[] afterBase64 = null;
		try {
		  afterBase64 = Base64.decodeBase64(encodedChart);
		} catch(Exception e) {
			notFound("This chart does not seem to exist");
		}

		byte[] result = new byte[length];
		try {
			// Decompress the bytes
			java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
			decompresser.setInput(afterBase64, 0, afterBase64.length);
			int resultLength = decompresser.inflate(result);
			decompresser.end();
		} catch(Exception e) {
			notFound("This chart does not seem to exist");
		}

		Chart chart = convert(version, result);
		return chart;
	}

	private static Chart convert(int version, byte[] data) {
		try {
			if(version == 1) {
				ObjectMapper mapper = new ObjectMapper(new BsonFactory());
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				Chart chart2 = mapper.readValue(bais, Chart.class);
				return chart2;
			} else {
				badRequest("Your graph is not found since this is the wrong version");
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}
}
