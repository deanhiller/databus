package gov.nrel.consumer.beans;

public class Delay {

	private int delay;
	private int interval;

	public Delay(int delay, int interval) {
		this.delay = delay;
		this.interval = interval;
	}

	public int getDelay() {
		return delay;
	}

	public int getInterval() {
		return interval;
	}

}
