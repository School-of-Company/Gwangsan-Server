package team.startup.gwangsan.adapter.web.member.dto.response;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.time.LocalDateTime;

public record FindAllUserInfoResponse(
        Long memberId,
        String nickname,
        String name,
        String phoneNumber,
        MemberRole role,
        MemberStatus status,
        LocalDateTime joinedAt,
        Integer gwangsan,
        String placeName
) {}


