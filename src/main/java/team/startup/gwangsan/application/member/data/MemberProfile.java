package team.startup.gwangsan.application.member.data;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

import java.time.LocalDateTime;

/** 회원·상세·지점 조인 조회 결과. 조회 경로에 따라 조인하지 않은 이름은 null 이다. */
public record MemberProfile(Long memberId, String nickname, String name, String phoneNumber, MemberRole role,
                            MemberStatus status, LocalDateTime joinedAt, String placeName, String headName,
                            String dongName, Integer light, Integer gwangsan, String description) {
}
