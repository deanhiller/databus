package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import play.mvc.results.NotFound;

import models.StreamAggregation;

import com.alvazan.play.NoSql;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;


public abstract class StreamsProcessor extends PullProcessorAbstract {

	protected Long currentTimePointer;
	protected List<PullProcessor> children = new ArrayList<PullProcessor>();
	protected List<ProxyProcessor> processors = new ArrayList<ProxyProcessor>();
	private List<String> urls;
	private List<ReadResult> results = new ArrayList<ReadResult>();

	@Override
	public String toString() {
		String msg = getClass().getSimpleName()+":"+children;
		return msg;
	}

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public void start(VisitorInfo visitor) {
		for(ProcessorSetup p : children) {
			p.start(visitor);
		}
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor) {
		String newPath = super.init(path, nextInChain, visitor);
		List<String> params2 = params.getParams();
		String name = params2.get(0);
		
		StreamAggregation agg = NoSql.em().find(StreamAggregation.class, name);
		if(agg == null)
			throw new NotFound("Aggregation named="+name+" was not found");
		urls = agg.getUrls();
		if(urls.size() == 0)
			throw new NotFound("Aggregation named="+name+" has not paths that we can read from");
		
		return newPath;
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup useThisChild, boolean alreadyAddedInverter) {
		long start = params.getOriginalStart();
		long end = params.getOriginalEnd();
		for(String url : urls) {
			String newUrl = addTimeStamps(url, start, end);
			ProcessorSetup child = super.createPipeline(newUrl, visitor, null, false);
			children.add((PullProcessor) child);
			processors.add(new ProxyProcessor((PullProcessor)child));
		}
		return null;
	}

	private String addTimeStamps(String url, long start, long end) {
		return url+"/"+start+"/"+end;
	}

	@Override
	public ReadResult read() {
		List<ReadResult> rows = readAllRows();
		if(rows == null)
			return null;
		
		long min = calculate(rows);
		
		List<TSRelational> toProcess = find(rows, min);
		results.clear();
		return process(toProcess);
	}

	private List<TSRelational> find(List<ReadResult> rows, long min) {
		List<TSRelational> toProcess = new ArrayList<TSRelational>();
		for(int i = 0; i < rows.size(); i++) {
			ProxyProcessor proxy = processors.get(i);
			ReadResult res = rows.get(i);
			if(res.getRow() != null) {
				long time = getTime(res.getRow());
				if(min == time) {
					proxy.read(); //make sure we read and discard the value we are about to process
					toProcess.add(res.getRow());
				} else
					toProcess.add(null);
			} else
				toProcess.add(null);
		}
		return toProcess;
	}

	private long calculate(List<ReadResult> rows) {
		long min = Long.MAX_VALUE;
		for(ReadResult res : rows) {
			if(res.getRow() != null) {
				long time = getTime(res.getRow());
				min = Math.min(time, min);
			}
		}
		return min;
	}

	private List<ReadResult> readAllRows() {
        int numChildrenRead = results.size();

       //This will pick up where we left off the first time....
       for(int i = numChildrenRead; i < children.size(); i++) {
    	   ProxyProcessor proc = processors.get(i);
    	   ReadResult result = proc.peek();
    	   if(result == null)
    		   return null;
    	   results.add(result);
       }
       return results;
	}



	protected abstract ReadResult process(List<TSRelational> results);



}
