package team.startup.gwangsan.domain.member.repository.projection;

import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;

import java.time.LocalDateTime;

public record FindAllUserDto(
        Long memberId,
        String nickname,
        String name,
        String phoneNumber,
        MemberRole role,
        MemberStatus status,
        LocalDateTime joinedAt,
        Integer gwangsan
) {
}