package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import models.SecureTable;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.mortbay.log.Log;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.play.NoSql;
import com.alvazan.play.NoSqlPlugin;

import play.Logger;
import play.Play;
import play.server.FileChannelBuffer;

/**
 * This is an override of the play StreamChunkAggregator so that we can make callouts for each chunk of a 
 * file upload rather than only uploading the entire file then handing it off to the controller once the 
 * upload is done.  We're dealing with possibly large files, we need to be processing then when we can 
 * simultaneously to uploading them.
 * 
 * @author jcollins
 *
 */

public class DatabusStreamChunkAggregator extends SimpleChannelUpstreamHandler {

    private volatile HttpMessage currentMessage;
    private volatile OutputStream out;
    private final static int maxContentLength = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));
    private volatile File file;
    private static List<String> chunkListenerClasses = new ArrayList<String>();
    private List chunkListenerInstances = new ArrayList();

    static {
    	try {
	    	//initialize the listener classes from config:
	    	//String chunkedListenerClassList = Play.configuration.getProperty("databus.chunked.listener.classes", "gov.nrel.util.ChunkedCSVUploadListener,gov.nrel.util.ChunkedJSONUploadListener");
    		String chunkedListenerClassList = Play.configuration.getProperty("databus.chunked.listener.classes", "controllers.ChunkedCSVUploader");
    		
	    	String[] listenerClass = chunkedListenerClassList.split(",");  
	        if(listenerClass.length != 0){
	            for (int i = 0; i < listenerClass.length; i++) {
	                String thisclass = listenerClass[i];
	                if (thisclass.length() != 0) {
		                try {
		                    chunkListenerClasses.add(thisclass);
		                } catch (Throwable e) {
		                	e.printStackTrace();
		                    Logger.error("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration.  ClassName: " + thisclass, e);
		                    throw new RuntimeException("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration.  ClassName: " + thisclass, e);
		                }
	                }
	            }
	        }
    	}
    	catch (Throwable t) {
    		Logger.error("static initialization of DatabusStreamChunkAggregator failed!", t);
    	}
        
    }
    
    /**
     * Creates a new instance.
     * @throws ClassNotFoundException 
     */
    public DatabusStreamChunkAggregator()  {
    	try {
	    	for (String listenerClass:chunkListenerClasses) {
            	Class c = Play.classloader.loadClass(listenerClass);
	    		chunkListenerInstances.add(c.newInstance());
	    	}
    	}
    	catch (IllegalAccessException iae) {
    		Logger.error("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", iae);
    		throw new RuntimeException("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", iae);
    	} catch (InstantiationException e) {
    		Logger.error("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", e);
    		throw new RuntimeException("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", e);
		} catch (ClassNotFoundException e) {
    		Logger.error("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", e);
    		throw new RuntimeException("Error adding a chunkedListener to the DatabusStreamChunkAggregator.  Check your databus.chunked.listener.classes configuration. ", e);
		}
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof HttpMessage) && !(msg instanceof HttpChunk)) {
            ctx.sendUpstream(e);
            return;
        }

        HttpMessage currentMessage = this.currentMessage;
        File localFile = this.file;
        if (currentMessage == null) {

            HttpMessage m = (HttpMessage) msg;
            if (m.isChunked()) {
                final String localName = UUID.randomUUID().toString();
                // A chunked message - remove 'Transfer-Encoding' header,
                // initialize the cumulative buffer, and wait for incoming chunks.
                List<String> encodings = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                this.currentMessage = m;
                this.file = new File(Play.tmpDir, localName);
                this.file.createNewFile();
                this.out = new FileOutputStream(file, true);
                m.addHeader("chunkedBufferFile", file.getAbsolutePath());

            } else {
                // Not a chunked message - pass through.
                ctx.sendUpstream(e);
            }
        } else {
            // TODO: If less that threshold then in memory
            // Merge the received chunk into the content of the current message.
            final HttpChunk chunk = (HttpChunk) msg;
            if (maxContentLength != -1 && (localFile.length() > (maxContentLength - chunk.getContent().readableBytes()))) {
                currentMessage.setHeader(HttpHeaders.Names.WARNING, "play.netty.content.length.exceeded");
            } else {
            	
                IOUtils.copyLarge(new ChannelBufferInputStream(chunk.getContent()), this.out);
                this.out.flush();
                try {
	                for (Object listener:chunkListenerInstances) {
	                	Method shouldNotifyMethod = listener.getClass().getMethod("shouldNotify", String.class);
	                	Method chunkMethod = listener.getClass().getMethod("chunk", File.class, String.class);
	
	                	//Method shouldNotifyMethod = Play.classloader.loadClass("server.DatabusStreamChunkAggregator$ChunkedListener").getMethod("shouldNotify", String.class);
	                	//Method chunkMethod = Play.classloader.loadClass("server.DatabusStreamChunkAggregator$ChunkedListener").getMethod("chunk", File.class, String.class);
	
	                	if ((Boolean)shouldNotifyMethod.invoke(listener,  ((DefaultHttpRequest)currentMessage).getUri())) {
	                		chunkMethod.invoke(listener, file, ((DefaultHttpRequest)currentMessage).getUri());
	                	}
	                }
                }
                catch (Exception ex) {
                	ex.printStackTrace();
                }
                if (chunk.isLast()) {
                    this.out.flush();
                    this.out.close();

                    Method shouldNotifyMethod = Play.classloader.loadClass("server.DatabusStreamChunkAggregator$ChunkedListener").getMethod("shouldNotify", String.class);
                	Method completeMethod = Play.classloader.loadClass("server.DatabusStreamChunkAggregator$ChunkedListener").getMethod("dataComplete");

                    for (Object listener:chunkListenerInstances) {
                    	if ((Boolean)shouldNotifyMethod.invoke(listener,  ((DefaultHttpRequest)currentMessage).getUri())) {
                    		completeMethod.invoke(listener);
                    	}
                    }

                    currentMessage.setHeader(
                            HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(localFile.length()));

                    currentMessage.setContent(new FileChannelBuffer(localFile));
                    this.out = null;
                    this.currentMessage = null;
                    this.file.delete();
                    this.file = null;
                    Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
                }
            }
        }

    }
    
    public interface ChunkedListener {
    	public boolean shouldNotify(String uri);
    	public void chunk(File sharedFile, String uri);
    	public void dataComplete();
    }
}

