package controllers.modules2.framework;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.TreeMap;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

public class TSRelational extends TreeMap<String, Object>{

	private static final long serialVersionUID = 1L;
	private String timeCol = "time";
	private String valCol = "value";
	
	//TODO:  all of the "time" and "value" strings should be setable, not hardcoded:
	public TSRelational() {
		super();
	}
	
	public TSRelational(TSRelational toCopy) {
		super(toCopy);
	}
	
	public TSRelational(BigInteger time, Object value) {
		this.put(timeCol, time);
		this.put(valCol, value);
	}
	
	public TSRelational(String timeColName, BigInteger time, String valueColName, Object value) {
		timeCol = timeColName;
		valCol = valueColName;
		this.put(timeColName, time);
		this.put(valueColName, value);
	}

	public long getTime() {
		BigInteger timeStr = (BigInteger) this.get("time");
		return timeStr.longValue();
	}

	public void setTime(long time) {
		this.put("time", new BigInteger(time+""));
	}

	public BigDecimal getValue(String col) {
		return (BigDecimal) get(col);
	}
	
	public String getTimeCol() {
		return timeCol;
	}

	public void setTimeCol(String timeCol) {
		this.timeCol = timeCol;
	}
	
	public String getValCol() {
		return valCol;
	}

	public void setValCol(String valCol) {
		this.valCol = valCol;
	}


}
