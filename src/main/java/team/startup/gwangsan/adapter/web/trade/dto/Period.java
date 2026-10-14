package team.startup.gwangsan.adapter.web.trade.dto;

public enum Period {
    DAY(1),
    WEEK(7),
    MONTH(30),
    YEAR(365);

    private final int value;

    Period(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
