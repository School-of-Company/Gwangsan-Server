package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.NotTradeCancelRequesterException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelWithdrawService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class TradeCancelWithdrawServiceImpl implements TradeCancelWithdrawService {

    private final MemberUtil memberUtil;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final TradeCancelImageRepository tradeCancelImageRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Member member = memberUtil.getCurrentMember();

        TradeComplete tradeComplete = tradeCompleteRepository.findByProductIdAndStatus(
                productId, TradeStatus.COMPLETED)
                .orElseThrow(NotFoundTradeCompleteException::new);

        TradeCancel tradeCancel = tradeCancelRepository.findByTradeCompleteIdAndStatus(tradeComplete.getId(), TradeCancelStatus.PENDING)
                .orElseThrow(NotFoundTradeCancelException::new);

        if (tradeCancel.getMember().getId().equals(member.getId())) {
            throw new NotTradeCancelRequesterException();
        }

        tradeCancel.updateStatus(TradeCancelStatus.WITHDRAWN);

        tradeCancelImageRepository.deleteByTradeCancelId(tradeCancel.getId());
    }
}
