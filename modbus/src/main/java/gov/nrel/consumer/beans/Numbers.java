package gov.nrel.consumer.beans;

public class Numbers {

	private long postTime;
	private long fullRegTime;
	private long reregisterTime;
	private long registerTime;
	private long postNewStreamTime;
	private long postDeviceTime;

	public void setPostTime(long total) {
		this.postTime= total;
	}

	public void setFullRegTime(long total) {
		this.fullRegTime = total;
	}

	public void setReregisterTime(long total) {
		this.reregisterTime = total;
	}

	public void setRegisterTime(long total) {
		this.registerTime = total;
	}

	public void setPostNewStreamTime(long total) {
		this.postNewStreamTime = total;
	}

	public void setPostDeviceTime(long total) {
		this.postDeviceTime = total;
	}

	public long getPostTime() {
		return postTime;
	}

	public long getFullRegTime() {
		return fullRegTime;
	}

	public long getReregisterTime() {
		return reregisterTime;
	}

	public long getRegisterTime() {
		return registerTime;
	}

	public long getPostNewStreamTime() {
		return postNewStreamTime;
	}

	public long getPostDeviceTime() {
		return postDeviceTime;
	}

}
