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
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class MyCharts extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyCharts.class);

	public static void createChart() {
		
		render();
	}
	
	public static void postCreateChart(Chart chart) throws UnsupportedEncodingException {

		Map<String, String[]> all = params.all();

		Map<String, String> props = new HashMap<String, String>();
		String myMap = convert(props);
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
		System.out.println("length="+encodedChart.length()+" result="+encodedChart);
		 
		drawChart(encodedChart, input.length);
	}

	private static String convert(Map<String, String> props) {
		return "";
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

		Map<String, String> props = convert(outputString);
		
		render(props);
	}

	private static Map<String, String> convert(String outputString) {
		return new HashMap<String, String>();
	}
}
