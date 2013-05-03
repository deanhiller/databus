package play.server;

import java.nio.ByteBuffer;

public class CircBuffer2 implements CircBuffer {

	private ByteBuffer buf;
	private int length;
	
	public CircBuffer2(int size) {
		buf = ByteBuffer.allocate(size);
		buf.flip();
	}

	@Override
	public int size() {
		return buf.capacity();
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public int put(byte[] chunk) {
		int startPosition = buf.position();
		int endPosition = buf.limit();
		//compact the buffer before every put operation...
		int roomLeft = buf.capacity()-endPosition;
		if(chunk.length > roomLeft) {
			buf.compact();
			buf.flip();
			startPosition = buf.position();
			endPosition = buf.limit();
		}
		
		//prepare for more writing
		buf.position(buf.limit());
		buf.limit(buf.capacity());
		buf.put(chunk);
		buf.limit(buf.position());
		buf.position(startPosition);
		
		length+=chunk.length;
		return chunk.length;
	}

	@Override
	public void mark() {
		buf.mark();
	}

	@Override
	public int get(byte[] data) {
		buf.get(data);
		length -= data.length;
		return data.length;
	}

	@Override
	public void reset() {
		buf.reset();
		calculateLength();
	}

	@Override
	public byte get() {
		length--;
		return buf.get();
	}

	@Override
	public void discardBytes(int length) {
		int pos = buf.position();
		buf.position(pos+length);
		calculateLength();
	}

	private void calculateLength() {
		length = buf.limit()-buf.position();
	}

	@Override
	public int nextGetPosition() {
		return buf.position();
	}

	@Override
	public void nextGetPosition(int pos) {
		buf.position(pos);
		calculateLength();
	}

	@Override
	public void clear() {
		buf.position(0);
		buf.limit(0);
		calculateLength();
	}

}
