package team.startup.gwangsan.domain.member.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Member {

    private final Long id;
    private final String name;
    private String nickname;
    private String passwordHash;
    private final String phoneNumber;
    private final Long recommenderId;
    private final MemberRole role;
    private final MemberStatus status;
    private final LocalDateTime joinedAt;

    /** 가입 신청 회원. 관리자 승인 전까지 PENDING 이다. */
    public static Member applicant(String name, String nickname, String passwordHash, String phoneNumber, Long recommenderId) {
        return new Member(null, name, nickname, passwordHash, phoneNumber, recommenderId,
                MemberRole.ROLE_USER, MemberStatus.PENDING, null);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
