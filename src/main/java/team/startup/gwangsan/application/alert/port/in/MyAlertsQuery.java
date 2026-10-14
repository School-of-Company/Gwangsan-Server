package team.startup.gwangsan.application.alert.port.in;

import team.startup.gwangsan.application.alert.data.AlertView;

import java.util.List;

public interface MyAlertsQuery {
    List<AlertView> query(Long memberId);
}
