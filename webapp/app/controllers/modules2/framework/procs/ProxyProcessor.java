package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.List;

import controllers.modules2.framework.ReadResult;

public class ProxyProcessor {

	private PullProcessor child;
	private List<ReadResult> cached = new ArrayList<ReadResult>();

	public ProxyProcessor(PullProcessor child) {
		this.child = child;
	}

	
	public ReadResult read() {
		if(cached.size() > 0) {
			return cached.remove(0);
		}
		ReadResult res = child.read();
		return res;
	}
	
	public ReadResult peek() {
		if(cached.size() > 0) {
			return cached.get(0);
		}
		ReadResult res = child.read();
		cached.add(res);
		return res;
	}
	
	public List<ReadResult> peekThree() {
		List<ReadResult> results = new ArrayList<ReadResult>();
		for(int i = 0; i < 3; i++) {
			ReadResult res = child.read();
			results.add(res);
			cached.add(res);
		}
		return results;
	}
}
