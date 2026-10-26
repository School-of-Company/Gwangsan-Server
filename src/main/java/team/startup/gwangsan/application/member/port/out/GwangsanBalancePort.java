package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

/** 회원 잔액의 원자적 갱신과 원장 기록. 모두 호출자 트랜잭션 안에서 실행된다. */
public interface GwangsanBalancePort {

    /** 원자적 가산. 반영 후 잔액을 반환한다. 대상 행이 없으면 NotFoundMemberDetailException. */
    int add(long memberId, int delta);

    /** SELECT gwangsan … FOR UPDATE. 관리자 조정의 하한 계산용. 대상 행이 없으면 NotFoundMemberDetailException. */
    int lockBalance(long memberId);

    /** 같은 (type, refId, memberId) 가 이미 있으면 유니크 위반으로 트랜잭션 전체가 롤백된다. */
    void record(long memberId, int amount, int balanceAfter, GwangsanLedgerType type, Long refId);
}
