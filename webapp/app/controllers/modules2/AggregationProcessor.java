package controllers.modules2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.StreamsProcessor;

public class AggregationProcessor extends StreamsProcessor {

	//map of url to it's mapped column name:
	private LinkedHashMap<String, String> entryNames = new LinkedHashMap<String, String>();
	
	private boolean dropIncomplete = true;
	private int lookaheadSize = 10;
	private boolean lookaheadSatisfied = false;
	private int largestRowWidth = -1;
	private List<ReadResult> lookaheadBuffer;
	
	@Override
	public String init(String path, ProcessorSetup nextInChain,
			VisitorInfo visitor, HashMap<String, String> options) {
		String res = super.init(path, nextInChain, visitor, options);
		for (String url:urls) {
			String entryName = getRawDataSource(url);
			if (entryNames.containsValue(entryName)) {
				Set<Entry<String, String>> entries = entryNames.entrySet();
				for (Entry<String, String> entry:entries) {
					if (entry.getValue() != null && entry.getValue().equals(entryName)) {
						String origEntry = entry.getKey();
						entryNames.remove(origEntry);
						entryNames.put(origEntry, origEntry);
						entryNames.put(url, url);
						break;
					}
				}
			}
			else
				entryNames.put(url, entryName);
		}
		
		String dropIncompleteStr = options.get("dropIncomplete");
		if (StringUtils.isNotBlank(dropIncompleteStr))
			dropIncomplete = Boolean.parseBoolean(dropIncompleteStr);
		
		String lookaheadSizeStr = options.get("lookaheadSize");
		if (StringUtils.isNotBlank(lookaheadSizeStr))
			lookaheadSize = Integer.parseInt(lookaheadSizeStr);

		return res;
	}
	
	@Override
	protected ReadResult process(List<TSRelational> rows) {
		Long timeCompare = null;
		BigDecimal total = BigDecimal.ZERO;
		TSRelational ts = new TSRelational();
		
		int index = 0;
		for(TSRelational row : rows) {
			if(row == null)
				continue;

			Long time = getTime(row);
			if(timeCompare == null) {
				timeCompare = time;
			} else if(!timeCompare.equals(time)) {
				throw new IllegalStateException("It appears you did not run spline before summing the streams as times are not lining up.  t1="+time+" t2="+timeCompare);
			}

			BigDecimal val = getValueEvenIfNull(row);
			if(val != null) {
				total = total.add(val);
			}
			ts.put(entryNames.get(urls.get(index++)), val);
		}

		if(timeCompare == null) {
			return new ReadResult();
		}
		
		setTime(ts, timeCompare);
		setValue(ts, total);
		ReadResult res = new ReadResult(null, ts);
		return res;
	}
	
	private int getLargestRowWidth(List<ReadResult> rows) {
		int result = 0;
		for (ReadResult r : rows) 
			result = Math.max(result, r.getRow().size());
		return result;
	}

	@Override
	public ReadResult read() {
		if (!dropIncomplete)
			return super.read();
		else {
			if (!lookaheadSatisfied) {
				lookaheadBuffer = new ArrayList<ReadResult>();
				ReadResult r = super.read();
				while (lookaheadBuffer.size() <= lookaheadSize && r != null && !r.isEndOfStream()) {
					lookaheadBuffer.add(r);
					r = super.read();
				}
				lookaheadBuffer.add(r);
				lookaheadSatisfied = true;
			}

			if (largestRowWidth < 0)
				largestRowWidth = getLargestRowWidth(lookaheadBuffer);
			while (CollectionUtils.size(lookaheadBuffer)!=0) {
				ReadResult RRreturn = lookaheadBuffer.get(0);
				lookaheadBuffer.remove(0);
				if (RRreturn != null && RRreturn.getRow() != null && RRreturn.isEndOfStream() && RRreturn.getRow().size() >=largestRowWidth)
					return RRreturn;
			}
			ReadResult r = super.read();
			while (r != null && r.getRow() != null && !r.isEndOfStream() && r.getRow().size() <largestRowWidth)
				r=super.read();
			return r;
		}
	}
	
	private String getRawDataSource(String url) {
		String apparentRawdata = StringUtils.substring(url, StringUtils.lastIndexOf(url, "/")+1);
		if (!StringUtils.isEmpty(apparentRawdata) && !StringUtils.contains(apparentRawdata, "/"))
			return apparentRawdata;
		return url;
	}
}
