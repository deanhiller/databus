package gov.nrel.util;

import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlVirtualCf;

@NoSqlEntity(columnfamily="nreldata")
@NoSqlVirtualCf(storedInCf="fakeTimeSeries")
public class FakeTimeSeries {

	@NoSqlId(usegenerator=false)
	private Long id;
	
	private double value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
}
