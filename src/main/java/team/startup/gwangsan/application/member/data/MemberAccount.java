package team.startup.gwangsan.application.member.data;

import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;

/** 인증·SMS 가 쓰는 회원 계정 정보. passwordHash 는 인코딩된 값이다. */
public record MemberAccount(Long id, String nickname, String phoneNumber, String passwordHash,
                            MemberRole role, MemberStatus status) {
}
