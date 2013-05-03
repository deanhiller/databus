package play.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class TestBoundaries {

	@Test
	public void testSomeCode() {
		String value = "multipart/form-data; boundary=----WebKitFormBoundaryuni3DI3VMQ0kPDKT";
		String[] values = value.split(";");
		
		String boundary = null;
		for(String val : values) {
			String theVal = val.trim();
			if(theVal.startsWith("boundary=")) {
				boundary = theVal.substring("boundary=".length());
				 break;
			}
		}
		System.out.println("boundary="+boundary);
	}
	
	@Test
	public void testBoundary() {
		String msg = 
				 "\r\n"+
			     "This is the preamble.  It is to be ignored, though it\r\n"+ 
			     "is a handy place for mail composers to include an\r\n" +
			     "explanatory note to non-MIME compliant readers."+ 
			     "--simple boundary"+ 
			     "\r\n\r\n"+
			     "This is implicitly typed plain ASCII text.\r\n"+ 
			     "It does NOT end with a linebreak."+ 
			     "\r\n--simple boundary\r\n"+ 
			     "Content-type: text/plain; charset=us-ascii"+ 
			     "\r\n\r\n"+
			     "This is explicitly typed plain ASCII text.\r\n"+ 
			     "It DOES end with a linebreak.\r\n"+ 
			     ""+
			     "\r\n--simple boundary--\r\n"+ 
			     "This is the epilogue.  It is also to be ignored.";
		
		byte[] bytes = msg.getBytes();
		
		MockDataListener l = new MockDataListener();
		ProcessStream str = new ProcessStream("\r\n--simple boundary", null, null);
		str.setChunkListener(l);
		//str.addDataListener(l);
		
		int size = 100;
		for(int i = 0; i < bytes.length; i+=size) {
			int s = Math.min(size, bytes.length-i);
			byte[] chunk = new byte[s];
			System.arraycopy(bytes, i, chunk, 0, chunk.length);
			
			str.addMoreData(chunk);
		}
	}
	
	private static class MockDataListener implements ChunkListener {
		private String fileContents = "";
		@Override
		public void moreFormParamData(ByteArrayInputStream in, Request request,
				Response response) {
			try {
				InputStreamReader r = new InputStreamReader(in);

				while(true) {
					int character = r.read();
					if(character < 0)
						break;
					char c = (char) character;
					fileContents += c;
				}
		    	System.out.println("file now="+fileContents);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public void startingFormParameter(String headers, Request request,
				Response response) {
			System.out.println("headers received="+headers);
		}
		@Override
		public void currentFormParamComplete(Request request, Response resp) {
		}
	}
}
