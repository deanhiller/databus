package controllers.gui.util;

import java.util.Comparator;

public class ChartComparator implements Comparator<ChartInfo> {

	@Override
	public int compare(ChartInfo chart1, ChartInfo chart2) {
		return chart1.getChartMeta().getName().compareTo(chart2.getChartMeta().getName());
	}

}
