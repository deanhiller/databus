package models;

import java.util.HashMap;
import java.util.Map;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;
import com.alvazan.orm.api.base.anno.NoSqlId;

import controllers.gui.util.ChartUtil;

@NoSqlEmbeddable
public class ChartDbo {
	
	@NoSqlId
	private String id = "";

	private String chartId;
	private String encodedVariables;
	private String title;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getChartId() {
		return chartId;
	}

	public void setChartId(String chartId) {
		this.chartId = chartId;
	}

	public String getEncodedVariables() {
		return encodedVariables;
	}

	public void setEncodedVariables(String encodedVariables) {
		this.encodedVariables = encodedVariables;
		Map<String, String> variables = ChartUtil.decodeVariables(encodedVariables);
		title = variables.get("title");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChartDbo other = (ChartDbo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String getTitle() {
		return title;
	}

	public String getSmallChartUrl() {
		return "/charts/smallchart/"+chartId+"/"+encodedVariables;
	}

	public String getLargeChartUrl() {
		return "/charts/largechart/"+chartId+"/"+encodedVariables;
	}
}
