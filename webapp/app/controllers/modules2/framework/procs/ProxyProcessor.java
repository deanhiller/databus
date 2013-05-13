package controllers.modules2.framework.procs;

import controllers.modules2.framework.ReadResult;

public class ProxyProcessor {

	private PullProcessor child;
	private ReadResult cached;

	public ProxyProcessor(PullProcessor child) {
		this.child = child;
	}

	
	public ReadResult read() {
		if(cached != null) {
			ReadResult res = cached;
			cached = null;
			return res;
		}
		ReadResult res = child.read();
		return res;
	}
	
	public ReadResult peek() {
		if(cached != null) {
			return cached;
		}
		cached = child.read();
		return cached;
	}
}
