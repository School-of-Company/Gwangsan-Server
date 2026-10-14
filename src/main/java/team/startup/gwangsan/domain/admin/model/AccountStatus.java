package team.startup.gwangsan.domain.admin.model;

// 회원 Entity 의 MemberStatus 와 같은 이름을 유지한다. 요청·저장 값이 이름 기준으로 매핑된다.
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    WITHDRAWN
}
