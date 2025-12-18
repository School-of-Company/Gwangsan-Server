package team.startup.gwangsan.domain.member.peresentation.dto.response;

import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;

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
