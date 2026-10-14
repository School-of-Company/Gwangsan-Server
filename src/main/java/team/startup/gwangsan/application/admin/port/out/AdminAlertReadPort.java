package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

import java.util.List;

/** 관리자 알림 목록 화면 조회. 기존 batch 조회·join 구성을 유지한다. */
public interface AdminAlertReadPort {
    AdminAlertsResult findAlerts(List<Integer> placeIds, AlertType type);
}
