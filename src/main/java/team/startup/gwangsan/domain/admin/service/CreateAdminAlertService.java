package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;

public interface CreateAdminAlertService {
    void execute(AlertType type, Long sourceId, Member member);
}
