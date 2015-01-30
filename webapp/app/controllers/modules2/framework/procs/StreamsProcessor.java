package controllers.modules2.framework.procs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.StreamAggregation;
import models.message.ChartVarMeta;
import models.message.StreamModule;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Http.Request;
import play.mvc.results.BadRequest;
import play.mvc.results.NotFound;

import com.alvazan.play.NoSql;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;


public abstract class StreamsProcessor extends PullProcessorAbstract {

	static final String DYNAMIC_SOURCE_PREFIX = "src";
	static final String DYNAMIC_SOURCE_NAME_PREFIX = "srcName";
	static final String RAW_DATA_MODULE_NAME = "rawdataV1";
	static final int MAX_DYNAMIC_SOURCES = 12;
	static final Logger log = LoggerFactory.getLogger(StreamsProcessor.class);

	protected Long currentTimePointer;
	protected List<ProxyProcessor> processors = new ArrayList<ProxyProcessor>();
	protected List<String> urls;
	protected List<String> urlNames;
	private List<ReadResult> results = new ArrayList<ReadResult>();
	private String aggName;
	private boolean dynamic = false;


	protected static Map<String, ChartVarMeta> parameterMeta = new HashMap<String, ChartVarMeta>();

	@Override
	public RowMeta getRowMeta() {
		List<String> columns = new ArrayList<String>();
		if (isDynamic()) {
			//it sucks doing nested for loops here, but there are only a few of them.
			//this ensures that the rare case where there are 2 sources with the same url key each get different names.
			for (int i = 0; i < urlNames.size(); i++) {				
				for(ProcessorSetup p : children) {
					if (p.getUrl().equals(urls.get(i))) {
						//if it has only 2 override the one thats not 'time'
						//if it has any number, override if one of them is 'value'
						//otherwise, give up and don't override the name
						RowMeta rowMeta = p.getRowMeta();
						if (rowMeta.getValueColumns().size()==1) {
							columns.add(rowMeta.getTimeColumn());
							columns.add(urlNames.get(i));
						}
						else if (rowMeta.getValueColumns().contains("value")) {
							for (String s : rowMeta.getValueColumns()) {
								if (!s.equals("value"))
									columns.add(s);
								else
									columns.add(urlNames.get(i));
							}
						}
							
					}
						
				}
			}
		}
		else {
			for(ProcessorSetup p : children) {
				RowMeta rowMeta = p.getRowMeta();
				columns.addAll(rowMeta.getValueColumns());
			}
		}
		
		RowMeta meta = new RowMeta("time", columns);
		return meta;
	}
	
	protected boolean isDynamic() {
		return dynamic;
	}

	protected void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
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
		if (dynamic)
			setupDynamicUrls(params2);
		else 
			setupStoredUrls(params2);
		
		
		return newPath;
	}
	
	private void setupStoredUrls(List<String> params2) {
		aggName = params2.get(0);
		
		StreamAggregation agg = NoSql.em().find(StreamAggregation.class, aggName);
		if(agg == null)
			throw new NotFound("Aggregation named="+aggName+" was not found");
		urls = agg.getUrls();
		if(urls.size() == 0)
			throw new NotFound("Aggregation named="+aggName+" has not paths that we can read from");
		
	}
	
	private void setupDynamicUrls(List<String> params2) {
		//the parameterized sources might need to be 'massaged' to be properly formatted, 
		//the 'rawdata' might have to move from the main url into each source url in some cases
		HashMap<String, String> sources = getParameterizedSources();
		
		//here are the cases when dealing with rawdata positions in dynamic aggregations, also strip unneeded 'api', and leading and trailing slashes:
		//http.../dynamicAggMod/splinesV2/.../rawdataV1/start/end?
		//src1=multiplyV1/1000/table1
		//src2=/cleanV1/1/100/rawdataV1/table2
		//src3=table3/
		//src4=api/rawdataV1/table4

		//desired final urls of substreams (the univeral url supplied after '../dynamicAggMod/<universal>/...') plus the part in each substream definition:
		//the last thing is ALWAY the table name, second to last must ALWAYS be 'rawdataV1'
		//...splinesV2/.../multiplyV1/1000/rawdataV1/table1
		//                                 ^^^^^^^^^^  inserted here, removed from base (or moved) rawdata
		//...splinesV2/.../cleanV1/1/100/rawdataV1/table2
		//                               ^^^^^^^^^^ no dup rawdata, first removed
		//...splinesV2/.../rawdataV1/table3
		//                 ^^^^^^^^^^   easy?  no changes?
		//...splinesV2/.../rawdataV1/table4
		//                 ^^^^^^^^^^   no duplicate ‘rawdata’
		urls = new ArrayList<String>();
		urlNames = new ArrayList<String>();

		String universalUrl = cleanDynamicUrlPart(params2.get(0));
		for (Entry<String, String> e:sources.entrySet()) {
			String sourceUrl = cleanDynamicUrlPart(e.getValue());
			sourceUrl = universalUrl+"/"+sourceUrl;
			
			String[] pieces = sourceUrl.split("/");
			String secondToLast = null;
			//pieces[pieces.length-1] is the table name...
			//pieces[pieces.length-2] needs to be 'rawdataV1'
			if(pieces.length >= 2) {
				secondToLast = pieces[pieces.length-2];
				if (!RAW_DATA_MODULE_NAME.equals(secondToLast)) {
					String start = StringUtils.substringBeforeLast(sourceUrl, "/");
					sourceUrl = start+"/"+RAW_DATA_MODULE_NAME+"/"+pieces[pieces.length-1];
				}
			}	
			
			while (StringUtils.countMatches(sourceUrl, "/"+RAW_DATA_MODULE_NAME) > 1) {
				sourceUrl = StringUtils.replaceOnce(sourceUrl, "/"+RAW_DATA_MODULE_NAME, "");
			}
			
			while (StringUtils.countMatches(sourceUrl, RAW_DATA_MODULE_NAME) > 1) {
				sourceUrl = StringUtils.replaceOnce(sourceUrl, RAW_DATA_MODULE_NAME, "");
			}
			e.setValue(sourceUrl);
			urls.add(sourceUrl);
			urlNames.add(e.getKey());
System.out.println("sourceUrl named "+e.getKey()+" is ");
System.out.println(sourceUrl);
		}
		
	}
	
	private String cleanDynamicUrlPart(String urlpart) {
		urlpart = StringUtils.removeEnd(urlpart, "/");
		urlpart = StringUtils.removeStart(urlpart, "/");
		urlpart = StringUtils.removeStart(urlpart, "api/");
		return urlpart;
	}
	
	private HashMap<String, String> getParameterizedSources() {
		HashMap<String, String> sources = new HashMap<String, String>();
		int i = 1;
		Request r = Request.current();
		for (;i < MAX_DYNAMIC_SOURCES; i++) {
			String src = r.params.get(DYNAMIC_SOURCE_PREFIX+i);
			String srcName = r.params.get(DYNAMIC_SOURCE_NAME_PREFIX+i);
			if (src != null) {
				if (srcName == null)
					srcName = src;
				sources.put(srcName, src);
			}
		}
		return sources;
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
		
		if (isDynamic()) {
			//if it has only 2 override the one thats not 'time'
			//if it has any number, override if one of them is 'value'
			//otherwise, give up and don't override the name
			for (int i = 0; i < urlNames.size(); i++) {
				for(ProcessorSetup p : children) {
					if (p.getUrl().equals(urls.get(i))) {
						RowMeta rowMeta = p.getRowMeta();
						if (rowMeta.getValueColumns().size()==1)
							rowMeta.setValueColumn(urlNames.get(i));
						else if (rowMeta.getValueColumns().contains("value")) {
							for (int j = 0; j < rowMeta.getValueColumns().size(); j++) {
								if (rowMeta.getValueColumns().get(j).equals("value")) {
									rowMeta.getValueColumns().set(j, urlNames.get(i));
								}
							}
						}
					}
				}
			}
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
    	   results.add(applyRowMeta(result, i));
       }
       return results;
	}



	private ReadResult applyRowMeta(ReadResult result, int i) {
		if (dynamic) {
			String name = urlNames.get(i);
			TSRelational row = result.getRow();
			//if it only has 'time' and 1 value, replace the value name
			//or if it has any number of vals, but one is 'value', replace its name
			if (row != null && (row.size() == 2 || row.containsKey("value"))){
				Object value = row.get(row.getValCol());
				row.remove(row.getValCol());
				row.setValCol(name);
				row.put(name, value);
			}
		}
		return result;
	}

	protected abstract ReadResult process(List<TSRelational> results);



}
