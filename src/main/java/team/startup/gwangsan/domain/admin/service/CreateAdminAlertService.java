package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

public interface CreateAdminAlertService {
    void execute(AlertType type, Long sourceId, Long memberId);
}
