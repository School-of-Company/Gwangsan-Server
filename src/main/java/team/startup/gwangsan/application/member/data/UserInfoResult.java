package team.startup.gwangsan.application.member.data;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;

public record UserInfoResult(Long memberId, String nickname, String placeName, Integer light, Integer gwangsan,
                             String description, List<String> specialties, String name, String phoneNumber,
                             MemberRole role, MemberStatus status, LocalDateTime joinedAt) {
}
