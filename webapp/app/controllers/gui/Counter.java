package controllers.gui;


public class Counter {

	private int counter;

	public void increment() {
		counter++;
	}

	public int getCount() {
		return counter;
	}

	@Override
	public String toString() {
		return counter + "";
	}

}
