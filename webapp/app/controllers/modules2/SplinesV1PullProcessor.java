package controllers.modules2;


public class SplinesV1PullProcessor extends SplinesPullProcessor {

	@Override
	protected int getNumParams() {
		return 2;
	}

	protected long calculateOffset() {
		Long startTime = params.getStart();
		if(startTime == null)
			return 0;
		long offset = startTime % interval;
		return offset;
	}

}
