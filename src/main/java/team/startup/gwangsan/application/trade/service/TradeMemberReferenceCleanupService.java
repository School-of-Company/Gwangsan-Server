package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.port.in.TradeMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeMemberReferencePort;

/** 회원 탈퇴 시 거래의 회원 참조 재지정. 탈퇴 트랜잭션에 참여한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class TradeMemberReferenceCleanupService implements TradeMemberReferenceCleanupUseCase {

    private final TradeMemberReferencePort tradeMemberReferencePort;

    @Override
    public void reassignTradeParties(Long memberId, Long dummyMemberId) {
        tradeMemberReferencePort.reassignBuyer(memberId, dummyMemberId);
        tradeMemberReferencePort.reassignSeller(memberId, dummyMemberId);
    }

    @Override
    public void reassignCancelRequester(Long memberId, Long dummyMemberId) {
        tradeMemberReferencePort.reassignCancelRequester(memberId, dummyMemberId);
    }
}
