package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.member.entity.constant.MemberRole;

public interface UpdateMemberRoleService {
    void execute(Long memberId, MemberRole role, Integer placeId);
}
