package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 철회 처리. 승인은 trade(PR07) 공개 command, 상품 ID·잠금은 trade/post(PR06) 공개 Port 를 쓴다.
 * legacy bridge: trade 에 기각·요청자 조회 공개 Port 가 없어 기각 상태 변경과 요청자 조회는 trade 저장 모델을 쓴다.
 * 제거: trade 기각 command·철회 요청자 query → PR12.
 */
@Component
@RequiredArgsConstructor
public class AdminTradeCancelBridge implements AdminTradeCancelPort {

    private final TradeCancelQuery tradeCancelQuery;
    private final ProductStatusUseCase productStatusUseCase;
    private final ApproveTradeCancelUseCase approveTradeCancelUseCase;
    private final TradeCancelJpaRepository tradeCancelRepository;

    @Override
    public Long productIdOf(Long tradeCancelId) {
        // 스칼라 조회만 한다. 철회 엔티티를 잠금 전에 읽지 않는다.
        return Optional.ofNullable(tradeCancelQuery.findProductIds(List.of(tradeCancelId)).get(tradeCancelId))
                .orElseThrow(NotFoundTradeCancelException::new);
    }

    @Override
    public void lockProduct(Long productId) {
        productStatusUseCase.lockIncludingDeleted(productId);
    }

    @Override
    public void approve(Long tradeCancelId, ParticipantCheck check) {
        approveTradeCancelUseCase.execute(new ApproveTradeCancelCommand(tradeCancelId, check::check));
    }

    @Override
    public Long rejectPending(Long tradeCancelId) {
        TradeCancelJpaEntity tradeCancel = tradeCancelRepository.findById(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new);
        if (tradeCancel.getStatus() != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }
        tradeCancel.updateStatus(TradeCancelStatus.REJECTED);
        return tradeCancel.getMember().getId();
    }

    @Override
    public Long requesterIdOf(Long tradeCancelId) {
        return tradeCancelRepository.findByIdWithMember(tradeCancelId)
                .orElseThrow(NotFoundTradeCancelException::new)
                .getMember().getId();
    }
}
