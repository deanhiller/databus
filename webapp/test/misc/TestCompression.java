package misc;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

public class TestCompression {

	@Test
	public void testSomething() throws UnsupportedEncodingException, DataFormatException {
		String url1 = "url1=http://databus.nrel.gov/api/module1/module2/something";
		String url2 = "url1=http://databus.nrel.gov/api/module1/module2/something";
		String url3 = "url1=http://databus.nrel.gov/api/module1/module2/something";
		String url4 = "url1=http://databus.nrel.gov/api/module1/module2/something";
		String all = url1+url2+url3+url4;
		
		System.out.println("length="+all.length());
		
		// Encode a String into bytes
		 byte[] input = all.getBytes("UTF-8");

		 // Compress the bytes
		 byte[] output1 = new byte[input.length];
		 Deflater compresser = new Deflater();
		 compresser.setInput(input);
		 compresser.finish();
		 int compressedDataLength = compresser.deflate(output1);

		 byte[] compressed2 = Arrays.copyOfRange(output1, 0, compressedDataLength);
		 String result2 = Base64.encodeBase64URLSafeString(compressed2);
		 System.out.println("length="+result2.length()+" result="+result2);

		 byte[] afterBase64 = Base64.decodeBase64(result2);
		 
		 System.out.println("len bytes="+afterBase64.length+" before="+compressed2.length+" output1="+output1.length+" compressLen="+compressedDataLength);
		 
		 // Decompress the bytes
		 java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
		 decompresser.setInput(afterBase64, 0, afterBase64.length);
		 byte[] result = new byte[input.length];
		 int resultLength = decompresser.inflate(result);
		 decompresser.end();

		 // Decode the bytes into a String
		 String outputString = new String(result, 0, resultLength, "UTF-8");
		 System.out.println("len="+outputString.length()+"  outstr="+outputString);
	}
}
