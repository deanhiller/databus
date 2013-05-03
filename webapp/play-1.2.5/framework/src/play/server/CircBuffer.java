package play.server;

public interface CircBuffer {

	int size();

	int length();

	int put(byte[] chunk);

	void mark();

	int get(byte[] data);

	void reset();

	byte get();

	void discardBytes(int length);

	int nextGetPosition();

	void nextGetPosition(int pos);

	void clear();

}
