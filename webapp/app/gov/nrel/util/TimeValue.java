package gov.nrel.util;

import java.math.BigDecimal;

public class TimeValue {

	private long time;
	private BigDecimal value;
	public TimeValue(long currentTimePointer, BigDecimal value2) {
		this.time = currentTimePointer;
		this.value = value2;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public BigDecimal getValue() {
		return value;
	}
	public void setValue(BigDecimal value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "{tv="+time+","+value+"}";
	}
	
	
}
