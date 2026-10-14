package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.trade.port.out.TradeMemberReferencePort;

/**
 * 기존 bulk 쿼리(clearAutomatically)를 그대로 쓴다. 앞 단계가 컨텍스트를 비우므로 회원 참조는 호출마다 새로 얻는다.
 */
@Component
@RequiredArgsConstructor
public class TradeMemberReferencePersistenceAdapter implements TradeMemberReferencePort {

    private final TradeCompleteJpaRepository tradeCompleteJpaRepository;
    private final TradeCancelJpaRepository tradeCancelJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void reassignBuyer(Long memberId, Long dummyMemberId) {
        tradeCompleteJpaRepository.reassignBuyer(member(memberId), member(dummyMemberId));
    }

    @Override
    public void reassignSeller(Long memberId, Long dummyMemberId) {
        tradeCompleteJpaRepository.reassignSeller(member(memberId), member(dummyMemberId));
    }

    @Override
    public void reassignCancelRequester(Long memberId, Long dummyMemberId) {
        tradeCancelJpaRepository.reassignMember(member(memberId), member(dummyMemberId));
    }

    private MemberJpaEntity member(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
