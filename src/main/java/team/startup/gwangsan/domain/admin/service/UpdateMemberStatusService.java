package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;

public interface UpdateMemberStatusService {
    void execute(Long memberId, MemberStatus status);
}
