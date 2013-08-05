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
		else if(index == 3)
			return chart3Id;
		return chart4Id;
	}

	public int getDashboardChartCount() {
		return dashboardChartCount;
	}

	public void setDashboardChartCount(int numberOfCharts) {
		this.dashboardChartCount = numberOfCharts;
	}

	public void setChart1Id(String chart1Id) {
		this.chart1Id = chart1Id;
	}

	public void setChart2Id(String chart2Id) {
		this.chart2Id = chart2Id;
	}

	public void setChart3Id(String chart3Id) {
		this.chart3Id = chart3Id;
	}

	public void setChart4Id(String chart4Id) {
		this.chart4Id = chart4Id;
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
