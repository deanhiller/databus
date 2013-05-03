package controllers.modules2.framework;

import java.math.BigInteger;
import java.util.HashMap;

import com.alvazan.orm.api.z8spi.conv.StandardConverters;

public class TSRelational extends HashMap<String, Object>{

	private static final long serialVersionUID = 1L;

	public long getTime() {
		BigInteger timeStr = (BigInteger) this.get("time");
		return timeStr.longValue();
	}

	public void setTime(long time) {
		this.put("time", new BigInteger(time+""));
	}
	
}
