package team.startup.gwangsan.domain.auth.model;

import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;

/** 인증에 필요한 회원 계정 정보. role 은 JWT 권한 이름 그대로다. */
public record Account(Long id, String nickname, String phoneNumber, String passwordHash, String role, AccountStatus status) {

    /** 로그인·토큰 재발급 공통 규칙. 승인 대기와 그 외 비활성 상태의 오류를 구분한다. */
    public void assertCanSignIn() {
        if (status == AccountStatus.PENDING) {
            throw new PendingApprovalException();
        }
        if (status != AccountStatus.ACTIVE) {
            throw new ForbiddenException();
        }
    }
}
