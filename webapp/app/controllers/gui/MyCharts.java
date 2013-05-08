package controllers.gui;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import play.mvc.results.NotFound;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);

	public static void createChart() {
		render();
	}
	
	public static void postCreateChart(Chart chart) throws UnsupportedEncodingException {
		String url = chart.getUrl();
		int index = url.lastIndexOf("/");
		String endTimeStr = url.substring(index+1);
		String left = url.substring(0, index);
		int startIndex = left.lastIndexOf("/");
		String startTimeStr = left.substring(startIndex+1);

		long endTime = convertLong(endTimeStr);
		long startTime = convertLong(startTimeStr);
		chart.setStartTime(startTime);
		chart.setEndTime(endTime);

		if(validation.hasErrors()) {
			validation.keep();
			flash.keep();
			params.flash();
			createChart();
		}
		
		String myMap = convert(chart);
		// Encode a String into bytes
		byte[] input = myMap.getBytes("UTF-8");

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
		drawChart(encodedChart, length);
	}

	private static long convertLong(String str) {
		try {
			return Long.parseLong(str);
		} catch(NumberFormatException e) {
			validation.addError("chart.url", "The url supplied must end with {startTime}/{endTime}");
			flash.error("The url supplied must end with {startTime}/{endTime} and does not");
		}
		return 0;
	}

	private static String convert(Chart chart) {
		String url = chart.getUrl();
		String timeCol = chart.getTimeColumn();
		String col1 = chart.getColumn1();
		String col2 = chart.getColumn2();
		String col3 = chart.getColumn3();
		String col4 = chart.getColumn4();
		String col5 = chart.getColumn5();
		
		String version = "V01";
		String all = version;
		all += timeCol+"|"+col1+"|"+col2+"|"+col3+"|"+col4+"|"+col5+"|"+url;
		return all;
	}

	public static void drawChart(String encodedChart, int length) {
		byte[] afterBase64 = null;
		try {
		  afterBase64 = Base64.decodeBase64(encodedChart);
		} catch(Exception e) {
			notFound("This chart does not seem to exist");
		}

		String outputString = null;
		try {
			byte[] result = new byte[length];
			// Decompress the bytes
			java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
			decompresser.setInput(afterBase64, 0, afterBase64.length);
			int resultLength = decompresser.inflate(result);
			decompresser.end();

			// Decode the bytes into a String
			outputString = new String(result, 0, resultLength, "UTF-8");
		} catch(Exception e) {
			notFound("This chart does not seem to exist");
		}

		Chart chart = convert(outputString);

		String url = chart.getUrl();
		

		long startTime = 0;
		long endTime = 500000;
		render(chart, startTime, endTime);
	}

	private static Chart convert(String outputString) {
		if(outputString.startsWith("V01")) {
			String leftOver = outputString.substring(3);
			String[] split = leftOver.split("\\|");
			if(7 != split.length)
				badRequest("Your graph is not found, url is misformed");
			
			Chart c = new Chart();
			c.setTimeColumn(split[0]);
			c.setColumn1(split[1]);
			c.setColumn2(split[2]);
			c.setColumn3(split[3]);
			c.setColumn4(split[4]);
			c.setColumn5(split[5]);
			c.setUrl(split[6]);
			return c;
		} else {
			badRequest("Your graph is not found since this is the wrong version");
		}
		return null;
	}
}
