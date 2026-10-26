package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

/**
 * 회원 광산 잔액 변경 공개 command. 거래·환불·보상·관리자 조정이 자기 트랜잭션 안에서 호출한다.
 * 잔액은 원자적 UPDATE 로 바꾸고, 같은 트랜잭션에서 원장 행을 남긴다.
 */
public interface ChangeGwangsanUseCase {

    /** from → to 이동. 두 행을 회원 ID 오름차순으로 갱신하고 원장 2행을 남긴다. */
    void transfer(long fromMemberId, long toMemberId, int amount, GwangsanLedgerType type, long refId);

    void credit(long memberId, int amount, GwangsanLedgerType type, long refId);

    /** 관리자 조정. 양수는 지급, 음수는 차감이며 결과가 음수여도 그대로 반영한다(PR #325 의도). */
    void adjust(long memberId, int delta);
}
