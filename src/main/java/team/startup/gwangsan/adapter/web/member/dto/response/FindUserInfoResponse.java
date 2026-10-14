package team.startup.gwangsan.adapter.web.member.dto.response;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;

public record FindUserInfoResponse(
        Long memberId,
        String nickname,
        String placeName,
        Integer light,
        Integer gwangsan,
        String description,
        List<String> specialties,
        String name,
        String phoneNumber,
        MemberRole role,
        MemberStatus status,
        LocalDateTime joinedAt
) {}
