package team.startup.gwangsan.domain.auth.model;

// 회원 상태 이름과 같다. auth 는 member 의 enum 을 직접 참조하지 않는다.
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    WITHDRAWN
}
