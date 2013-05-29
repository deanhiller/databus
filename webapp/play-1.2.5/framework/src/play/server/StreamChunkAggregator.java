package play.server;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class StreamChunkAggregator extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(StreamChunkAggregator.class);
    private volatile HttpMessage currentMessage;
    private final int maxContentLength;
    //There should be no file stuff and they should let the application deal with it especially
    //since we do not want the huge performance hit of writing to local disk!!!!
    //We want to stream the file to 6 different disks so we can keep up with the streaming and not take a hit
    //of writing to disk then copying(plus they write it to disk not once but twice :( :( )
    //getting rid of the file writes took it from 45 seconds to 3 seconds upload of 1Gig file.
    //private volatile OutputStream out;
    //private volatile File file;
	private int totalBytes;
	private File file;
	private FileOutputStream out;

    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if(log.isTraceEnabled())
        	log.trace("message received="+msg.getClass().getSimpleName());
        if (!(msg instanceof HttpMessage) && !(msg instanceof HttpChunk)) {
            ctx.sendUpstream(e);
            return;
        }

        HttpMessage currentMessage = this.currentMessage;
        File localFile = this.file;
        if (currentMessage == null) {
            totalBytes = 0;
            
            HttpMessage m = (HttpMessage) msg;
        	if(log.isTraceEnabled())
        		log.trace("currentmsg=null and msg chunked="+m.isChunked());
            if (m.isChunked()) {
            	if (log.isInfoEnabled())
                	log.info("it's a chunked request");
            	
                final String localName = UUID.randomUUID().toString();
                // A chunked message - remove 'Transfer-Encoding' header,
                // initialize the cumulative buffer, and wait for incoming chunks.
                List<String> encodings = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                this.currentMessage = m;

                if(!specialUpload(m)) {
                	this.file = new File(Play.tmpDir, localName);
                	this.out = new FileOutputStream(file, true);
                } else //if special upload, send the first piece upstream...
                	ctx.sendUpstream(e);
            } else {
                // Not a chunked message - pass through.
                ctx.sendUpstream(e);
            }
        } else {
        	if(log.isTraceEnabled())
        		log.trace("no currentmessage, must be in memory");
            // TODO: If less that threshold then in memory
            // Merge the received chunk into the content of the current message.
            final HttpChunk chunk = (HttpChunk) msg;
            totalBytes += chunk.getContent().readableBytes();
            if (maxContentLength != -1 && totalBytes > maxContentLength) { 
            	if(log.isTraceEnabled())
            		log.trace("length exceeded?");
                currentMessage.setHeader(HttpHeaders.Names.WARNING, "play.netty.content.length.exceeded");
            } else {
            	if(log.isTraceEnabled())
            		log.trace("process chunk");
                
            	if(!specialUpload(currentMessage)) 
            		IOUtils.copyLarge(new ChannelBufferInputStream(chunk.getContent()), this.out);
            	else {
            		OurChunk c = new OurChunk(chunk, currentMessage);
            		UpstreamMessageEvent evt = new UpstreamMessageEvent(e.getChannel(), c, e.getRemoteAddress());
            		ctx.sendUpstream(evt);
            	}

                if (chunk.isLast()) {
                	if(log.isTraceEnabled())
                		log.trace("this is the last chunk");
                	
                	if(!specialUpload(currentMessage)) {
	                    this.out.flush();
	                    this.out.close();
	
	                    currentMessage.setHeader(
	                            HttpHeaders.Names.CONTENT_LENGTH,
	                            String.valueOf(localFile.length()));
	
	                    currentMessage.setContent(new FileChannelBuffer(localFile));
	                    this.out = null;
	                    this.currentMessage = null;
	                    this.file.delete();
	                    this.file = null;
                	}
                    Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
                }
            }
        }

    }

	private boolean specialUpload(HttpMessage m) {
		if(m instanceof HttpRequest) {
			HttpRequest r = (HttpRequest) m;
			String uri = r.getUri();
			if(uri.endsWith("specialupload"))
				return true;
		}
		return false;
	}
}

