package models.comparitors;

import java.util.Comparator;

import models.UserChart;

public class UserChartComparitor  implements Comparator<UserChart> {
    @Override
    public int compare(UserChart o1, UserChart o2) {
        return o1.getChartName().toLowerCase().compareTo(o2.getChartName().toLowerCase());
    }
}