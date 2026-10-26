package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.out.GwangsanBalancePort;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

/**
 * 잔액은 거래 상태·환불·알림과 함께 커밋돼야 하므로 호출자 트랜잭션을 필수로 요구한다(MANDATORY).
 * 단독 호출은 거절되어 잔액만 따로 커밋되는 경로를 만들지 않는다.
 *
 * <p>잔액은 원자적 UPDATE 로 바꾼다. 상품 잠금만으로는 같은 회원의 서로 다른 거래가 직렬화되지 않아
 * 엔티티 값을 읽고 쓰면 잔액이 유실된다(ADR-001). 음수 잔액 허용은 기존 정책이다. 관리자 조정만 0 하한을 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ChangeGwangsanService implements ChangeGwangsanUseCase {

    private final GwangsanBalancePort gwangsanBalancePort;

    /** 회원 ID 오름차순으로 갱신한다. 원자적 UPDATE 도 행 잠금을 커밋까지 쥐므로 순서가 없으면 맞거래가 데드락난다. */
    @Override
    public void transfer(long fromMemberId, long toMemberId, int amount, GwangsanLedgerType type, long refId) {
        if (fromMemberId < toMemberId) {
            apply(fromMemberId, -amount, type, refId);
            apply(toMemberId, amount, type, refId);
        } else {
            apply(toMemberId, amount, type, refId);
            apply(fromMemberId, -amount, type, refId);
        }
    }

    @Override
    public void credit(long memberId, int amount, GwangsanLedgerType type, long refId) {
        apply(memberId, amount, type, refId);
    }

    @Override
    public void adjust(long memberId, int delta) {
        int current = gwangsanBalancePort.lockBalance(memberId);
        int applied = Math.max(0, current + delta) - current;
        apply(memberId, applied, GwangsanLedgerType.ADMIN_ADJUST, null);
    }

    private void apply(long memberId, int delta, GwangsanLedgerType type, Long refId) {
        int balanceAfter = gwangsanBalancePort.add(memberId, delta);
        gwangsanBalancePort.record(memberId, delta, balanceAfter, type, refId);
    }
}
