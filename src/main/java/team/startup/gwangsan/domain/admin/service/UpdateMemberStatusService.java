package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.member.model.MemberStatus;

public interface UpdateMemberStatusService {
    void execute(Long memberId, MemberStatus status);
}
