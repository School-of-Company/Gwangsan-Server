package team.startup.gwangsan.domain.common;

import java.util.Objects;

/**
 * 인증된 요청 실행자. adapter/security 가 신뢰된 principal 에서만 만든다.
 * 요청 body/query 의 회원 ID 를 실행자로 사용하지 않는다.
 */
public record ActorContext(Long memberId, Role role) {

    public ActorContext {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(role, "role");
    }

    // 회원 Entity 의 MemberRole 과 같은 이름을 유지한다. 권한 문자열(SecurityConfig)과 매핑이 이름 기준이다.
    public enum Role {
        ROLE_USER,
        ROLE_PLACE_ADMIN,
        ROLE_HEAD_ADMIN
    }
}
