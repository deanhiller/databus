package gov.nrel.modbusclient;

public class Info {

	private Long value;
	private String command;

	public Info(Long value) {
		this.value = value;
	}

	public Info(String cmd, Long value) {
		this.command = cmd;
		this.value = value;
	}

	@Override
	public String toString() {
		return "Info [value=" + value + ", command=" + command + "]";
	}
	

}
