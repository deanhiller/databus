package controllers.gui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alvazan.play.NoSql;

import play.mvc.Controller;
import play.mvc.With;
import play.mvc.results.NotFound;
import controllers.gui.auth.GuiSecure;
import de.undercouch.bson4jackson.BsonFactory;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);

	public static void createChart() {
		render();
	}
	
	public static void postCreateChart(Chart chart) throws UnsupportedEncodingException {
		String url = chart.getUrl();
		int index = url.lastIndexOf("/");
		if(index < 0) {
			validation.addError("chart.url", "The url supplied must end with {startTime}/{endTime}");
			flash.error("The url supplied must end with {startTime}/{endTime} and does not");
		} else {
			String endTimeStr = url.substring(index+1);
			String left = url.substring(0, index);
			int startIndex = left.lastIndexOf("/");
			String startTimeStr = left.substring(startIndex+1);
	
			long endTime = convertLong(endTimeStr);
			long startTime = convertLong(startTimeStr);
			chart.setStartTime(startTime);
			chart.setEndTime(endTime);
		}

		if(validation.hasErrors()) {
			validation.keep();
			flash.keep();
			params.flash();
			createChart();
		}
		
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
		drawChart(encodedChart, 1, length);
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

	public static void drawChart(String encodedChart, int version, int length) {
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

		String url = chart.getUrl();
		

		long startTime = 0;
		long endTime = 500000;
		render(chart, startTime, endTime);
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
