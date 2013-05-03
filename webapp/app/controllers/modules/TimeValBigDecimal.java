package controllers.modules;

import java.math.BigDecimal;

public class TimeValBigDecimal {

	private long time;
	private BigDecimal value;
	
	public long getTime() {
		return time;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}
	
}
