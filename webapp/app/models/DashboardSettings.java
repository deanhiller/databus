package models;

import com.alvazan.orm.api.base.anno.NoSqlEmbeddable;

@NoSqlEmbeddable
public class DashboardSettings {

	private int dashboardChartCount = 4;
	private String chart1Id;
	private String chart2Id;
	private String chart3Id;
	private String chart4Id;

	public String getChartId(int index) {
		if(index == 0)
			return chart1Id;
		else if(index == 1)
			return chart2Id;
		else if(index == 2)
			return chart3Id;
		return chart4Id;
	}

	public int getDashboardChartCount() {
		return dashboardChartCount;
	}

	public void setDashboardChartCount(int numberOfCharts) {
		this.dashboardChartCount = numberOfCharts;
	}

	public void setChart1Id(String chartId) {
		if("-1".equals(chartId)) {
			this.chart1Id = null;
			return;
		}
		this.chart1Id = chartId;
	}

	public void setChart2Id(String chartId) {
		if("-1".equals(chartId)) {
			this.chart2Id = null;
			return;
		}
		this.chart2Id = chartId;
	}

	public void setChart3Id(String chartId) {
		if("-1".equals(chartId)) {
			this.chart3Id = null;
			return;
		}
		this.chart3Id = chartId;
	}

	public void setChart4Id(String chartId) {
		if("-1".equals(chartId)) {
			this.chart4Id = null;
			return;
		}
		this.chart4Id = chartId;
	}

	public String getChart1Id() {
		if(chart1Id == null)
			return "-1";
		return chart1Id;
	}

	public String getChart2Id() {
		if(chart2Id == null)
			return "-1";
		return chart2Id;
	}

	public String getChart3Id() {
		if(chart3Id == null)
			return "-1";
		return chart3Id;
	}

	public String getChart4Id() {
		if(chart4Id == null)
			return "-1";
		return chart4Id;
	}
	
}
