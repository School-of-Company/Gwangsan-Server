package team.startup.gwangsan.adapter.persistence.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.out.GwangsanBalancePort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

/**
 * 잔액은 엔티티 값이 아니라 UPDATE … SET gwangsan = gwangsan + :delta 로 바꾼다.
 * 반영 후 잔액은 1차 캐시의 낡은 엔티티가 아니라 스칼라 쿼리로 다시 읽는다(행 잠금을 쥔 상태라 정확하다).
 */
@Component
@RequiredArgsConstructor
public class GwangsanBalancePersistenceAdapter implements GwangsanBalancePort {

    private final MemberDetailJpaRepository memberDetailJpaRepository;
    private final GwangsanLedgerJpaRepository gwangsanLedgerJpaRepository;

    @Override
    public int add(long memberId, int delta) {
        if (memberDetailJpaRepository.addGwangsan(memberId, delta) == 0) {
            throw new NotFoundMemberDetailException();
        }
        return memberDetailJpaRepository.findGwangsan(memberId).orElseThrow(NotFoundMemberDetailException::new);
    }

    @Override
    public int lockBalance(long memberId) {
        return memberDetailJpaRepository.findGwangsanForUpdate(memberId).orElseThrow(NotFoundMemberDetailException::new);
    }

    @Override
    public void record(long memberId, int amount, int balanceAfter, GwangsanLedgerType type, Long refId) {
        gwangsanLedgerJpaRepository.save(GwangsanLedgerJpaEntity.of(memberId, amount, balanceAfter, type, refId));
    }
}
