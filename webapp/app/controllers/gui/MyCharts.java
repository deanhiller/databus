package controllers.gui;

import gov.nrel.util.Utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

import models.ChartDbo;
import models.EntityUser;
import models.ScriptChart;
import models.message.ChartMeta;
import models.message.ChartPageMeta;
import models.message.RegisterMessage;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import play.vfs.VirtualFile;
import controllers.Parsing;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.Chart;
import controllers.gui.util.ChartInfo;
import controllers.gui.util.ChartUtil;
import controllers.gui.util.Info;
import de.undercouch.bson4jackson.BsonFactory;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static void createChart() {
		String chartId = ChartInfo.BUILT_IN_CHART1;
		String encoded = "start";
		render(chartId, encoded);
	}

	public static void modifyChart(String chartId, String encoded) {
		Chart chart = deserialize(encoded);
		render("@createChart", chartId, encoded, chart);
	}

	public static void postStep1(String chartId, String encoded, Chart chart) throws UnsupportedEncodingException {
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
		encoded = createUrl(chartId, chart, 1);

		createStep2(chartId, encoded);
	}
	
	public static void createStep2(String chartId, String encoded) {
		Chart chart = deserialize(encoded);
		render(chartId, encoded, chart);
	}

	public static void postStep2(String chartId, String encoded, Chart chart) throws UnsupportedEncodingException {
		chart.fillIn();
		encoded = createUrl(chartId, chart, 2);
		drawChart(chartId, encoded);
	}
	
	public static void drawChart(String chartId, String encoded) {
		Chart chart = deserialize(encoded);
		int height = 0;
		render(chartId, encoded, chart, height);
	}
	
	public static void drawJustChart(String chartId, String encoded) {
		Chart chart = deserialize(encoded);
		int height = 0;
		String heightStr = params.get("height");
		if(heightStr != null) {
			String heightEsc = JavaExtensions.escapeJavaScript(heightStr);
			height = Integer.parseInt(heightEsc);
		}
		render(chartId, encoded, chart, height);
	}
	
	private static String createUrl(String chartId, Chart chart, int stepNumber) {
		String encoded;
		try {
			encoded = mapper.writeValueAsString(chart);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		byte[] bytes = encoded.getBytes();
		encoded = Base64.encodeBase64URLSafeString(bytes);
		
		if(log.isInfoEnabled())
			log.info("length="+encoded.length()+" result="+encoded);
		
		if(validation.hasErrors()) {
			flash.error("You have errors in your form below");
			validation.keep();
			flash.keep();
			params.flash();
			if(stepNumber == 1)
				createChart();
			else
				createStep2(chartId, encoded);
		}
		
		return encoded;
	}

	private static Long convertLong(String str) {
		try {
			return Long.parseLong(str);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	static Chart deserialize(String encodedChart) {
		try {
			byte[] decoded = Base64.decodeBase64(encodedChart);
			String json = new String(decoded);
			return mapper.readValue(json, Chart.class);
		} catch (JsonParseException e) {
			throw new RuntimeException(e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void addChartToDashboard(String chartId, String encoded) {
		Chart info = deserialize(encoded);
		
		ChartDbo chart = new ChartDbo();
		chart.setChartId(chartId);
		chart.setEncodedVariablesRaw(encoded);
		chart.setBuiltin(true);
		//he could create multiples of the same chart so just timestamp it as he would not
		//be fast enough to create ones with an id clash...
		chart.setId(chartId+System.currentTimeMillis());
		chart.setTitle(info.getTitle());
		EntityUser user = Utility.getCurrentUser(session);
		List<ChartDbo> charts = user.getCharts();
		charts.add(chart);
		NoSql.em().put(user);
		NoSql.em().flush();
		
		Settings.charts();
	}
}
