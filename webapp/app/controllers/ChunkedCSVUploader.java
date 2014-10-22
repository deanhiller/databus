package controllers;

public class ChunkedCSVUploader extends ChunkedUploader {
	@Override
	public boolean shouldNotify(String uri) {
		if (uri != null && uri.contains("api/postcsv"))
			return true;
		return false;
	}
}
