package play.server;

public interface HttpChunkStream {

	void addMoreData(byte[] data, boolean isLast);

	void setChunkListener(ChunkListener chunkListener);

}
