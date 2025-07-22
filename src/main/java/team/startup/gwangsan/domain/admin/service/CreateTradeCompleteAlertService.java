package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

public interface CreateTradeCompleteAlertService {
    void execute(AlertType type, Long sourceId, Long otherMemberId, Long requesterMemberId);
}
