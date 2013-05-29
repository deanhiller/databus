package play.server;

import java.io.ByteArrayInputStream;

import org.slf4j.LoggerFactory;

import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;


public class MultiPartStream implements HttpChunkStream {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(MultiPartStream.class);
	private ChunkListener listener;
	private CircBuffer buffer = new CircBuffer2(16384);
	private byte[] boundaryX;
	private static byte[] headerEnd = "\r\n\r\n".getBytes();
	private static byte[] afterBoundaryEndChunking = "--\r\n".getBytes();
	private byte[] currentDelimeter;
	
	private byte[] temporary = new byte[buffer.size()];
	private boolean inPreamble = true;
	//after every boundary, we need to read in headers up to the \r\n\r\n piece!!!
	private String headers = "";
	private Request request;
	private Response resp;
	private boolean isEndOfStream;

	public MultiPartStream(Request req, Response resp, String[] values) {
		String boundary = null;
		for(String val : values) {
			String theVal = val.trim();
			if(theVal.startsWith("boundary=")) {
				boundary = "\r\n--"+theVal.substring("boundary=".length());
				 break;
			}
		}
		init(boundary, req, resp);
	}
	
	public MultiPartStream(String boundary, Request req, Response resp) {
		init(boundary, req, resp);
	}

	private void init(String boundary, Request req, Response resp) {
		this.boundaryX = boundary.getBytes();
		//here we strip off the \r\n since chrome at least has no \r\n preceding the --boundary
		String pre = boundary.substring(2);
		byte[] preambleBoundary = pre.getBytes();
		//the first delimeter we look for is the boundary one..
		currentDelimeter = preambleBoundary;
		this.request = req;
		this.resp = resp;
	}

	private boolean isReadingInHeader() {
		if(currentDelimeter == headerEnd)
			return true;
		return false;
	}

	@Override
	public void addMoreData(byte[] chunk, boolean isLast) {
		if(isEndOfStream)
			return; //discard any more data
		else if(buffer.length()+chunk.length > buffer.size())
			throw new IllegalStateException("This chunk is too large to handle="+chunk.length+" contents of buffer="+buffer.length()+" max="+buffer.size());
		
		buffer.put(chunk);

		//WARNING: The currentDelimeter changes from the boundary delim(what browser sent us) to
		//the end of headers \r\n\r\n so we stay in this while loop such that we can switch between them all
		//just fine but currentDelimeter is changing in processBuffer and below flipping back and forth
		while(buffer.length() >= currentDelimeter.length) {
			processBuffer();
		}
		
		if(isLast && !isEndOfStream()) {
			log.warn("This should not occur but did, client may have sent malformed ending");
			listener.currentFormParamComplete(request, resp);
		}
	}

	private void processBuffer() {
		logCurrentBuffer();
		if(inPreamble) {
			//according to spec, all stuff up to first boundary is supposed to be discarded
			discardAllDataToBoundary();
			return;
		} else if(isReadingInHeader()) {
			if(isEndOfStream()) {
				isEndOfStream = true;
				buffer.clear();
				listener.currentFormParamComplete(request, resp);
				return;
			}
			
			if(!readInAllHeaders())
				return;
		}

		Integer numBytes = findNextBoundary(currentDelimeter);
		if(numBytes == null) {//if no boundary found, then we get how many e can write now
			//NOTE: We can't write al the bytes we have since some of the bytes could include the half the boundary so
			//we must keep boundary.length-1 bytes....we don't need boundary.length bytes since we know that did not match already
			numBytes = getAllBytesWeCan();
		} else {
			copyDataToTemp(numBytes);
			currentDelimeter = headerEnd;
		}
		if(numBytes > 0) {
			ByteArrayInputStream in = new ByteArrayInputStream(temporary, 0, numBytes);
			listener.moreFormParamData(in, request, resp);
		}
	}

	private void logCurrentBuffer() {
		if(log.isDebugEnabled()) {
			buffer.mark();
			
			byte[] data = new byte[buffer.length()];
			buffer.get(data);
			buffer.reset();
			
			String s = new String(data);
			
			if (log.isDebugEnabled())
        		log.debug(s);
		}
	}

	private boolean readInAllHeaders() {
		Integer numBytes = findNextBoundary(currentDelimeter);
		if(numBytes == null) {
			int size = getAllBytesWeCan();
			headers += new String(temporary, 0, size);
			return false;
		}

		copyDataToTemp(numBytes);
		headers += new String(temporary, 0, numBytes);

		listener.startingFormParameter(headers, request, resp);
		headers = ""; //clear the headers now
		currentDelimeter = boundaryX;
		return true;
	}

	private void copyDataToTemp(Integer numBytes) {
		for(int i = 0; i < numBytes; i++) {
			temporary[i] = buffer.get();
		}
		//discard the boundary
		buffer.discardBytes(currentDelimeter.length);
	}

	private boolean isEndOfStream() {
		if(buffer.size() < afterBoundaryEndChunking.length)
			return false;
		buffer.mark();
		for(int i = 0; i < afterBoundaryEndChunking.length; i++) {
			if(buffer.get() != afterBoundaryEndChunking[i]) {
				buffer.reset();
				return false;
			}
		}
		
		return true;
	}

	private Integer findNextBoundary(byte[] delimeter) {
		buffer.mark();

		Integer matchIndex = null;
		for(int byteCnt = 0; buffer.length() >= delimeter.length; byteCnt++) {
			int pos = buffer.nextGetPosition();
			for(int n = 0; n < delimeter.length; n++) {
				byte b = buffer.get();
				byte b2 = delimeter[n];
				if(b != b2) {
					break;
				} else if(n+1 == delimeter.length) {
					buffer.reset();
					if (log.isInfoEnabled())
	                	log.info("found a boundary math.  boundarysize="+delimeter.length);
					return byteCnt;
				} else {
					if (log.isInfoEnabled())
						log.info("keep comparing, may have boundary. n="+n+" delimsize="+delimeter.length);
				}
			}
			
			if(matchIndex == null) {
				//reset to last nextGetPos
				buffer.nextGetPosition(pos);
				//now discard a byte so we read the next byte
				buffer.discardBytes(1);
			}
		}

		buffer.reset();
		return null;
	}
	
	private void discardAllDataToBoundary() {
		Integer numBytesToBoundary = findNextBoundary(currentDelimeter);
		if(numBytesToBoundary == null) {
			//drop data we don't need anymore for streaming
			buffer.discardBytes(buffer.length()-currentDelimeter.length+1);
			return;
		}

		inPreamble = false;
		//drop number of bytes to boundary plus the entire boundary itself...
		buffer.discardBytes(numBytesToBoundary+currentDelimeter.length);
		currentDelimeter = headerEnd;
	}
	
	private int getAllBytesWeCan() {
		int i = 0;
		while(buffer.length() >= currentDelimeter.length) {
			temporary[i] = buffer.get();
			i++;
		}
		
		return i;
	}

	public void setChunkListener(ChunkListener l) {
		this.listener = l;
	}
}
