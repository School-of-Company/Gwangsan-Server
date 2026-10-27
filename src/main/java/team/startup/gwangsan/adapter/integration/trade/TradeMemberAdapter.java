package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.in.ChangeGwangsanUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.trade.port.out.TradeMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.GwangsanLedgerType;

/**
 * trade → member 공개 Port 연결. 잔액 변경(ChangeGwangsanUseCase)은 MANDATORY 라 거래 트랜잭션 안에서만 실행된다.
 */
@Component
@RequiredArgsConstructor
public class TradeMemberAdapter implements TradeMemberPort {

    private final MemberAccountQuery memberAccountQuery;
    private final ChangeGwangsanUseCase changeGwangsanUseCase;

    @Override
    public void requireMember(long memberId) {
        if (memberAccountQuery.findById(memberId).isEmpty()) {
            throw new NotFoundMemberDetailException();
        }
    }

    /** 회원 상세가 없으면 NotFoundMemberDetailException 으로 거래 전체가 rollback 된다. */
    @Override
    public void transferForCompletion(long fromMemberId, long toMemberId, int gwangsan, long tradeCompleteId) {
        changeGwangsanUseCase.transfer(fromMemberId, toMemberId, gwangsan, GwangsanLedgerType.TRADE_COMPLETE, tradeCompleteId);
    }

    @Override
    public void transferForCancel(long fromMemberId, long toMemberId, int gwangsan, long tradeCancelId) {
        changeGwangsanUseCase.transfer(fromMemberId, toMemberId, gwangsan, GwangsanLedgerType.TRADE_CANCEL, tradeCancelId);
    }
}
