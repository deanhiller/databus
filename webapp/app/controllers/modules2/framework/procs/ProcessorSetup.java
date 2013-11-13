package controllers.modules2.framework.procs;

import java.util.List;
import java.util.Map;

import models.message.StreamModule;

import controllers.modules2.framework.Direction;
import controllers.modules2.framework.VisitorInfo;

public interface ProcessorSetup {

	void createTree(ProcessorSetup parent, StreamModule info, VisitorInfo visitor);

	String init(String path, ProcessorSetup processorSetup, VisitorInfo visitor, Map<String, String> options);
	ProcessorSetup createPipeline(String path, VisitorInfo visitor, ProcessorSetup useThisChild, boolean alreadyAddedInverter);
	
	void start(VisitorInfo visitor);

	String getUrl();
	
	//NOTE: we got a little luck that our first module valuestojson is only push because otherwise
	//an engine might not get wired in at all.
	//manymodules  - Sink-get parent, src-EITHER (basically module does push/pull on both sink/src...so can be wired anywhere)
	//valuestojson - Sink-PUSH,       src-NONE
	//negation -     Sink-get parent, src-EITHER
	//engine -       Sink-PULL,       src-PUSH
	//spline -       Sink-PULL,       src-PULL
	//rawdata -      Sink-NONE,       src-PULL
	//buffer -       Sink-PUSH,       src-PULL
    //remoteproces - Sink-NONE,       src-PUSH
	Direction getSinkDirection();
	Direction getSourceDirection();

	
	List<String> getAggregationList();

	RowMeta getRowMeta();

	// Y Y N N N
	// N Y Y Y Y
	// Y N N Y Y
	// N N Y Y N
	// N N Y N N
	// N Y Y N N
    // Y N N N Y

	//valuestojson - no engine, has push method,  no pull method,  not pull data, not push data 
	//negation -     no engine, has push method, has pull method, will pull data, will push data
	//engine -      has engine,  no push method,  no pull method, will pull data, will push data
	//spline -       no engine,  no push method, has pull method, will pull data, not push data
	//rawdata -      no engine,  no push method, has pull method,  not pull data, not push data
	//buffer -       no engine, has push method, has pull method,  not pull data, not push data
    //remoteproces -has engine,  no push method,  no pull method,  not pull data, will push data

	//valuetoj - no engine, has push method,  no pull method, not pull data, not push data
	//negation - no engine, has push method, has pull method, wil pull data, wil push data
	//engine -  has engine,  no push method,  no pull method, wil pull data, wil push data
	//spline -   no engine,  no push method, has pull method, wil pull data, not push data
	//rawdata -  no engine,  no push method, has pull method, not pull data, not push data

	//valuestojson - no engine, has push method,  no pull method,  not pull data, not push data 
	//negation -     no engine, has push method, has pull method, will pull data, will push data
	//engine -      has engine,  no push method,  no pull method, will pull data, will push data
	//spline -       no engine,  no push method, has pull method, will pull data, not push data
	//buffer -       no engine, has push method, has pull method,  not pull data, not push data
    //remoteproces -has engine,  no push method,  no pull method,  not pull data, will push data

	//valuestojson - no engine, has push method,  no pull method,  not pull data, not push data 
	//negation -     no engine, has push method, has pull method, will pull data, will push data
	//invert -       no engine, has push method, has pull method, will pull data, will push data
    //remoteproces -has engine,  no push method,  no pull method,  not pull data, will push data

	//willPullData
	//wantsDataPush
	//
	
	//1. willPullData=false, wantsDataPush=true            sourceState=NONE
	//2. willPullData=ask parent, wantsDataPush=ask parent sourceState=ANYTHING
	//3. willPullData=true, wantsDataPush=false            sourceState=PUSH_ONLY
	//4. willPullData=true, wantsDataPush=false            sourceState=PULL_ONLY
	//5. willPullData=false, wantsDataPush=false           sourceState=PULL_ONLY
	//7. willPullData=false, wantsDataPush=false...........sourceState=PUSH_ONLY
	
	// 0. Y Y Y Y  many modules (pass through all questions up and answer isSinkXXX with "anything")
	// 1. Y N N N  valuestojson - on bottom 2(if engine),3,7
	// 2. Y Y Y Y  negation - on bottom 3,4,5,6,7 on top 1,3,4,6
	// 4. N Y Y N  spline - on bottom 5,6,2 on top 3,2

	// 5. N Y N N  rawdata - on top 4(only if have engine),3,2(only if have engine)
    // 7. N N N Y  remoteprocess on top 1,2,6 on bottom none
	
	// 3. N N Y Y  engine - on bottom 2,4,6 on top 1,2,6
	// 6. Y Y N N  buffer on bottom 7,3, and 2(only if have engine) on top 4,3,2(only if have engine)

	//valuestojson - has push method,  no pull method,  not pull data, not push data 
	//negation -     has push method, has pull method, will pull data, will push data
	//engine -        no push method,  no pull method, will pull data, will push data
	//spline -        no push method, has pull method, will pull data, not push data
	//rawdata -       no push method, has pull method,  not pull data, not push data
	//buffer -       has push method, has pull method,  not pull data, not push data
    //remoteproces -  no push method,  no pull method,  not pull data, will push data

	//valuetoj - has push method,  no pull method, not pull data, not push data
	//negation - has push method, has pull method, wil pull data, wil push data
	//engine -    no push method,  no pull method, wil pull data, wil push data
	//spline -    no push method, has pull method, wil pull data, not push data
	//rawdata -   no push method, has pull method, not pull data, not push data

	//valuestojson - has push method,  no pull method,  not pull data, not push data 
	//negation -     has push method, has pull method, will pull data, will push data
	//engine -        no push method,  no pull method, will pull data, will push data
	//spline -        no push method, has pull method, will pull data, not push data
	//buffer -       has push method, has pull method,  not pull data, not push data
    //remoteproces -  no push method,  no pull method,  not pull data, will push data

	//valuestojson - has push method,  no pull method,  not pull data, not push data 
	//negation -     has push method, has pull method, will pull data, will push data
	//invert -       has push method, has pull method, will pull data, will push data
    //remoteproces -  no push method,  no pull method,  not pull data, will push data
}
