package team.startup.gwangsan.domain.alert.service;

import team.startup.gwangsan.domain.alert.entity.constant.AlertType;

import java.util.List;

public interface CreateAlertMembersService {
    void execute(Long sourceId, List<Long> memberIds, AlertType alertType);
}
