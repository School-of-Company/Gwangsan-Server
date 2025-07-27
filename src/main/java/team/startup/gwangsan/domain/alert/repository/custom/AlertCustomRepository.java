 package team.startup.gwangsan.domain.alert.repository.custom;

import team.startup.gwangsan.domain.alert.entity.Alert;

import java.util.List;

public interface AlertCustomRepository {
    List<Alert> findUnreadAlerts(Long alertId, Long memberId);
}
