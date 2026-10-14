package team.startup.gwangsan.application.member.port.in;

/**
 * 회원 광산 잔액 변경 공개 command. 거래·환불·관리자 조정이 자기 트랜잭션 안에서 호출한다.
 * 잔액 행 잠금은 하지 않는다(기존 동작). 동시성은 호출자의 상품 잠금 순서에 의존한다.
 */
public interface ChangeGwangsanUseCase {

    void plus(Long memberId, int amount);

    void minus(Long memberId, int amount);

    /** 관리자 조정. 결과가 음수면 0 으로 맞춘다. */
    void adjust(Long memberId, int delta);
}
