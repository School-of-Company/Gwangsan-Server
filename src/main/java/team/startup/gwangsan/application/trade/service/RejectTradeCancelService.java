package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.port.in.RejectTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;

/**
 * 관리자 기각. 승인·자동 철회·요청 취소와 같은 상품 행 잠금 뒤에 호출돼야 이미 승인된 철회를 REJECTED 로 덮어쓰지 않는다.
 * 잠금과 격리 수준은 호출자(관리자 기각 트랜잭션)가 소유하고 여기서는 그 트랜잭션에 참여한다.
 */
@Service
@RequiredArgsConstructor
public class RejectTradeCancelService implements RejectTradeCancelUseCase {

    private final TradeCancelPort tradeCancelPort;

    @Override
    @Transactional
    public long reject(long tradeCancelId) {
        TradeCancel tradeCancel = tradeCancelPort.findBasicById(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);
        tradeCancel.reject();
        tradeCancelPort.update(tradeCancel);
        return tradeCancel.requesterId();
    }
}
