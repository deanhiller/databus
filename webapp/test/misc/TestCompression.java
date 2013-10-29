package misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

import controllers.gui.util.Chart;
import de.undercouch.bson4jackson.BsonFactory;

public class TestCompression {

	//@Test
	public void testSomething() throws DataFormatException, JsonGenerationException, JsonMappingException, IOException {
		String url1 = "url1=http://databus.nrel.gov/api/module1/module2/something";

		Chart chart = new Chart();
		//chart.setColumn1("temp");
		//chart.setColumn2("volume");
		//chart.setColumn3("number");
		//chart.setColumn4("energy");

		String all = url1+"column1="+chart.getSeries1().getName()+"&column2="+chart.getSeries2().getName()+"&column3="+chart.getSeries3().getName()+"&column4="+chart.getSeries4().getName();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectMapper mapper = new ObjectMapper(new BsonFactory());
		mapper.writeValue(baos, chart);

		// Encode a String into bytes
		byte[] input = baos.toByteArray();

		// Compress the bytes
		byte[] output1 = new byte[input.length];
		Deflater compresser = new Deflater();
		compresser.setInput(input);
		compresser.finish();
		int compressedDataLength = compresser.deflate(output1);

		System.out.println("input len="+input.length+" output len="+compressedDataLength);
		byte[] compressed2 = Arrays.copyOfRange(output1, 0, compressedDataLength);
		String result2 = Base64.encodeBase64URLSafeString(compressed2);
		System.out.println("url param length=" + result2.length() + " original len="+all.length());
		System.out.println("url param="+result2);
		byte[] afterBase64 = Base64.decodeBase64(result2);

		System.out.println("len bytes=" + afterBase64.length + " before="
				+ compressed2.length + " output1=" + output1.length
				+ " compressLen=" + compressedDataLength);

		// Decompress the bytes
		java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
		decompresser.setInput(afterBase64, 0, afterBase64.length);
		byte[] result = new byte[input.length];
		int resultLength = decompresser.inflate(result);
		decompresser.end();

		ByteArrayInputStream bais = new ByteArrayInputStream(result);
		Chart chart2 = mapper.readValue(bais, Chart.class);

		System.out.println("chart url="+"deleted"+" col1="+chart2.getSeries1().getName());
	}
}
