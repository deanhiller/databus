package gov.nrel.consumer.beans;

public class MultiplyConfig {

	private int random;
	private int multiply;

	public MultiplyConfig(int multiply, int random) {
		this.multiply = multiply;
		this.random = random;
	}

	public int getRandom() {
		return random;
	}

	public int getMultiply() {
		return multiply;
	}

}
