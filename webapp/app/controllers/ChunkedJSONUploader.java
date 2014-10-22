package controllers;

public class ChunkedJSONUploader extends ChunkedUploader {
	@Override
	public boolean shouldNotify(String uri) {
		if (uri != null && uri.contains("api/postjson"))
			return true;
		return false;
	}
}
