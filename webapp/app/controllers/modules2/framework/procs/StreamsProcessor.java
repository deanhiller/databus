package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.StreamAggregation;
import models.message.ChartVarMeta;
import models.message.StreamModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.results.BadRequest;
import play.mvc.results.NotFound;

import com.alvazan.play.NoSql;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;


public abstract class StreamsProcessor extends PullProcessorAbstract {

	static final Logger log = LoggerFactory.getLogger(StreamsProcessor.class);

	protected Long currentTimePointer;
	protected List<ProxyProcessor> processors = new ArrayList<ProxyProcessor>();
	protected List<String> urls;
	private List<ReadResult> results = new ArrayList<ReadResult>();
	private String aggName;

	protected static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();

	@Override
	public RowMeta getRowMeta() {
		List<String> columns = new ArrayList<String>();
		for(ProcessorSetup p : children) {
			RowMeta rowMeta = p.getRowMeta();
			columns.addAll(rowMeta.getValueColumns());
		}
		
		RowMeta meta = new RowMeta("time", columns);
		return meta;
	}

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
			VisitorInfo visitor, Map<String, String> options) {
		String newPath = super.init(path, nextInChain, visitor, options);
		List<String> params2 = params.getParams();
		aggName = params2.get(0);
		
		StreamAggregation agg = NoSql.em().find(StreamAggregation.class, aggName);
		if(agg == null)
			throw new NotFound("Aggregation named="+aggName+" was not found");
		urls = agg.getUrls();
		if(urls.size() == 0)
			throw new NotFound("Aggregation named="+aggName+" has not paths that we can read from");
		
		return newPath;
	}
	
	@Override
	public List<String> getAggregationList() {
		List<String> aggregationList;
		if (parent != null) 
			aggregationList = parent.getAggregationList();
		else 
			aggregationList = new ArrayList<String>();
		aggregationList.add(aggName);
		return aggregationList;
	}

	
	@Override
	public void createTree(ProcessorSetup parent, StreamModule thisNodeInfo, VisitorInfo visitor) {
		super.createTree(parent, thisNodeInfo, visitor);
		
		urls = new ArrayList<String>();
		for(int i = 0; i < children.size(); i++) {
			ProcessorSetup child = children.get(i);
			processors.add(new ProxyProcessor((PullProcessor)child));
			this.urls.add("id"+i);
		}
	}

	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup useThisChild, boolean alreadyAddedInverter) {
		Long start = params.getOriginalStart();
		Long end = params.getOriginalEnd();
		List<String> aggregationList = parent.getAggregationList();
		if (aggregationList.contains(aggName)) {
			aggregationList.add(aggName);
			throw new BadRequest("Your aggregation is trying to do an infinite loop back to itself.  List of urls:"+aggregationList);
		}
		else if (aggregationList.size() > 5) {
			aggregationList.add(aggName);
			log.info("Your aggregation is very deep, consider restructuring it!  List of urls: "+aggregationList);
		}
		

		for(String url : urls) {
			String newUrl = addTimeStamps(url, start, end);
			ProcessorSetup child = super.createPipeline(newUrl, visitor, null, false);
			processors.add(new ProxyProcessor((PullProcessor)child));
		}
		return null;
	}

	private String addTimeStamps(String url, Long start, Long end) {
		if(url.startsWith("/"))
			url = url.substring(1);
		return url+"/"+start+"/"+end;
	}

	@Override
	public ReadResult read() {
		List<ReadResult> rows = readAllRows();
		if(rows == null)
			return null;
		
		ReadResult res = fetchBadIndexAndRemove(rows);
		if(res != null) {
			results.clear();
			return res;
		}
		
		long min = calculate(rows);
		
		List<TSRelational> toProcess = find(rows, min);
		clearResults();
		return process(toProcess);
	}
	
	private ReadResult fetchBadIndexAndRemove(List<ReadResult> rows) {
		for(int i = 0; i < rows.size(); i++) {
			ReadResult res = rows.get(i);
			if(res.isMissingData()) {
				ProxyProcessor proc = processors.get(i);
				proc.read(); //make sure we read and discard the value
				return res;
			}
		}
		return null;
	}

	protected void clearResults() {
		results.clear();
	}

	protected List<TSRelational> find(List<ReadResult> rows, long min) {
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

	protected long calculate(List<ReadResult> rows) {
		long min = Long.MAX_VALUE;
		for(ReadResult res : rows) {
			if(res.getRow() != null) {
				long time = getTime(res.getRow());
				min = Math.min(time, min);
			}
		}
		return min;
	}

	protected List<ReadResult> readAllRows() {
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
