package team.startup.gwangsan.domain.alert.service;

import team.startup.gwangsan.domain.alert.entity.constant.AlertType;

public interface CreateAlertService {
    void execute(Long sourceId, Long memberId, AlertType alertType);
}
