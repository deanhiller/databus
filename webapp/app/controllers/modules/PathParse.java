package controllers.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathParse {
	private static final Logger log = LoggerFactory.getLogger(PathParse.class);
	
	public PathParse(){
	}
	/**
	 * Calculate the start and end time from the path
	 * @param path
	 * @return [0] = start time, [1] = end time
	 */
	public long[] calculateStartEnd(String path){
		long[] out = new long[2];
		
		int index;
		
		// Strip off any passed parameters
		index = path.lastIndexOf("?");
		if(index > 0)
			path = path.substring(0,index);
		
		// Now get the start and end times.
		index = path.lastIndexOf("/");
		
		String endTime = path.substring(index+1);
		String leftOver = path.substring(0, index);
		int index2 = leftOver.lastIndexOf("/");
		String startTime = leftOver.substring(index2+1);

		if (log.isInfoEnabled())
			log.info("parsing path="+path+" ind="+index+" ind2="+index2+" leftOver="+leftOver+" starttime="+startTime);
		long start = Long.parseLong(startTime);
		long end = Long.parseLong(endTime);
		
		out[0] = start;
		out[1] = end;
		
		return out;
	}
}
