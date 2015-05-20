package controllers.modules2;

import java.util.List;
import java.util.Map;

import play.mvc.results.BadRequest;
import controllers.modules.util.OldSuccessfulAbort;
import controllers.modules2.framework.Path;
import controllers.modules2.framework.ProcessedFlag;
import controllers.modules2.framework.SuccessfulAbort;
import controllers.modules2.framework.TSRelational;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.PushProcessor;
import controllers.modules2.framework.procs.PushProcessorAbstract;

public class FirstValuesProcessor extends PushProcessorAbstract {

	//basically, a byte is -128 to 127(long is similar) so we support -127 to 127 such that someone can negate either value and they are valid
	//If we used Long.MIN_VALUE instead of MIN_VALUE+1, we would get exceptions when someone negates the value which happens when reverse=true
	private long BEGIN_OF_TIME = Long.MIN_VALUE+1;
	//private long BEGIN_OF_TIME = 0; //let's have begin of time be 1970 for now, okay, change our mind, let's not
	private long numValues;
	private long counter = 0;

	@Override
	protected int getNumParams() {
		return 1;
	}

	@Override
	public String init(String pathStr, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		super.init(pathStr, nextInChain, visitor, options);
		//params = parsePath(pathStr, visitor);
		//first values may have no start and end times here
		long oneHundredYearsInMillis = 3153600000000L;
		long customEnd = System.currentTimeMillis() + oneHundredYearsInMillis;
		if(params.getOriginalStart() == null && params.getOriginalEnd() == null) {
			long end= customEnd;
			Long start  = null;
			String previousPath = params.getPreviousPath()+"/"+start+"/"+end;
			String leftOver = params.getLeftOverPath()+"/"+start+"/"+end;
			params = new Path(params.getParams(), previousPath, leftOver, start, end, visitor.isReversed());
		} else if(params.getOriginalStart() == null) {
			if(params.getOriginalEnd() < 0)
				throw new RuntimeException("The time cannot be less than 0, ie. cannot be before 1970");
			//strip off previouspath and leftOVer path's single time value from the url here!!!!
			String previousPath = strip(params.getPreviousPath());
			String leftOver = strip(params.getLeftOverPath());
			//in this case, there is only one time in the url which was put at the end parameter :( which is confusing
			//and to add to the confusion if reverse=true, end actually means end but reverse=false end means start instead
			if(!visitor.isReversed()) {
				long end = customEnd;
				previousPath = previousPath+"/"+params.getOriginalEnd()+"/"+end;
				leftOver = leftOver+"/"+params.getOriginalEnd()+"/"+end;
				params = new Path(params.getParams(), previousPath, leftOver, params.getOriginalEnd(), end, visitor.isReversed());
			} else {
				Long start  = null;
				previousPath = previousPath+"/"+start+"/"+params.getOriginalEnd();
				leftOver = leftOver+"/"+start+"/"+params.getOriginalEnd();
				params = new Path(params.getParams(), previousPath, leftOver, start, params.getOriginalEnd(), visitor.isReversed());
			}
		}

		String msg = "After the /firstvalues/ in the url must be a long value of how many data points you want returned";
		numValues = parseLong(params.getParams().get(0), msg);
		return params.getLeftOverPath();
	}

	private String strip(String previousPath) {
		int lastIndex = previousPath.lastIndexOf("/");
		return previousPath.substring(0, lastIndex);
	}

	@Override
	public void incomingChunk(String url, TSRelational row, ProcessedFlag flag) {
		if(counter >= numValues) {
			throw new SuccessfulAbort("We are done processing and have enough data for first values module");
		}
		
		counter++;
		getNextInChain().incomingChunk(url, row, flag);
	}

}
