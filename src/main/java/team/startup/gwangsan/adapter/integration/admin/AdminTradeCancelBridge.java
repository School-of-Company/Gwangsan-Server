package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.RejectTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 철회 처리. trade(PR07) 공개 Port(승인·기각 command, 상품 ID·요청자 query)와 post(PR06) 잠금 command 를 쓴다.
 * 승인 잠금 순서(상품 ID 스칼라 → 상품 잠금 → 철회 최초 로드)는 trade 승인 command 가, 기각은 호출자가 잠근 뒤 reject 를 부른다.
 */
@Component
@RequiredArgsConstructor
public class AdminTradeCancelBridge implements AdminTradeCancelPort {

    private final TradeCancelQuery tradeCancelQuery;
    private final ProductStatusUseCase productStatusUseCase;
    private final ApproveTradeCancelUseCase approveTradeCancelUseCase;
    private final RejectTradeCancelUseCase rejectTradeCancelUseCase;

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
        return rejectTradeCancelUseCase.reject(tradeCancelId);
    }

    @Override
    public Long requesterIdOf(Long tradeCancelId) {
        return tradeCancelQuery.findRequesterId(tradeCancelId).orElseThrow(NotFoundTradeCancelException::new);
    }
}
