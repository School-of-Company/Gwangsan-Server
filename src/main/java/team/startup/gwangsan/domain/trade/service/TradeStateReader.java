package team.startup.gwangsan.domain.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 거래 상태를 읽는 단일 지점.
 *
 * <p>채팅방 조회 API와 거래 상태 변경 이벤트 발행부가 모두 이곳을 거치게 해서,
 * 두 경로가 같은 상태를 다르게 계산하는 일이 구조적으로 생기지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class TradeStateReader {

    private final TradeCompleteRepository tradeCompleteRepository;

    public TradeStateSnapshot read(Product product, Member buyer, Member seller) {
        Optional<TradeComplete> pending = tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING);

        return new TradeStateSnapshot(
                product.getStatus() == ProductStatus.COMPLETED,
                product.getStatus() == ProductStatus.RESERVATION,
                // 대기 중인 요청이 있을 때만 값을 갖는다. 롤백된 요청의 값이 새어 나가면
                // 클라이언트의 isCompletable 계산이 조회 응답과 어긋난다.
                pending.map(TradeComplete::isRequestedBySeller).orElse(null),
                resolveRequestedAt(product, buyer, seller, pending)
        );
    }

    /**
     * 거래 카드를 대화 흐름에 배치하고 노출 여부를 정하는 기준 시각.
     *
     * <p>대기 중인 요청이 있으면 그 요청 시각, 없으면 완료된 요청의 시각을 쓴다.
     * 완료 후에도 값이 남아야 거래 카드가 유지되어 완료 표시와 리뷰 작성 진입점이 보인다.
     *
     * <p>반대로 철회가 승인된 뒤에는 null 이어야 한다. 값이 남으면 클라이언트가 아직
     * 거래 요청이 있다고 판단해 재요청 버튼을 잠근다.
     */
    private LocalDateTime resolveRequestedAt(Product product, Member buyer, Member seller,
                                             Optional<TradeComplete> pending) {
        if (pending.isPresent()) {
            return pending.get().getCreatedAt();
        }
        return tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.COMPLETED)
                .map(TradeComplete::getCreatedAt)
                .orElse(null);
    }
}
