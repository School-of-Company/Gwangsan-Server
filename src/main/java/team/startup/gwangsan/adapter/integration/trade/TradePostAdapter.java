package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.PendingReservationUseCase;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.trade.data.TradeImage;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;
import team.startup.gwangsan.application.trade.port.out.TradeReservationPort;
import team.startup.gwangsan.domain.trade.model.ProductPhase;
import team.startup.gwangsan.domain.trade.model.TradeProduct;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * trade → post 공개 Port 연결. 잠금·상태 변경은 호출자 트랜잭션에 참여한다(PR06 계약).
 */
@Component
@RequiredArgsConstructor
public class TradePostAdapter implements TradeProductPort, TradeReservationPort {

    private final ProductStatusUseCase productStatusUseCase;
    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;
    private final PendingReservationUseCase pendingReservationUseCase;

    @Override
    public TradeProduct lockActive(long productId) {
        return toTradeProduct(productStatusUseCase.lockActive(productId));
    }

    @Override
    public TradeProduct lockIncludingDeleted(long productId) {
        return toTradeProduct(productStatusUseCase.lockIncludingDeleted(productId));
    }

    /** 삭제 포함, 같은 트랜잭션에서 이미 바뀐 상태를 포함해 읽는다. */
    @Override
    public TradeProduct find(long productId) {
        return toTradeProduct(productSnapshotQuery.get(productId));
    }

    @Override
    public void complete(long productId) {
        productStatusUseCase.complete(productId);
    }

    @Override
    public void reopen(long productId) {
        productStatusUseCase.reopen(productId);
    }

    @Override
    public Map<Long, TradeImage> findRepresentativeImages(List<Long> productIds) {
        // ponytail: 이미지 순서 컬럼이 없어 연결 행 조회 순서상 첫 장을 대표로 쓴다(기존과 같음).
        Map<Long, TradeImage> images = new LinkedHashMap<>();
        productImageQuery.findImages(productIds).forEach((productId, views) -> {
            if (!views.isEmpty()) {
                ImageView first = views.get(0);
                images.put(productId, new TradeImage(first.imageId(), first.imageUrl()));
            }
        });
        return images;
    }

    @Override
    public long requireParticipantReservation(long productId, long buyerId, long sellerId) {
        return pendingReservationUseCase.requireParticipant(productId, buyerId, sellerId);
    }

    @Override
    public void completeReservation(long reservationId) {
        pendingReservationUseCase.complete(reservationId);
    }

    private static TradeProduct toTradeProduct(ProductSnapshot snapshot) {
        return new TradeProduct(snapshot.id(), snapshot.authorId(), snapshot.isGiver(), phase(snapshot), snapshot.gwangsan());
    }

    private static ProductPhase phase(ProductSnapshot snapshot) {
        return switch (snapshot.status()) {
            case ONGOING -> ProductPhase.ONGOING;
            case RESERVATION -> ProductPhase.RESERVED;
            case COMPLETED -> ProductPhase.COMPLETED;
            case DELETED -> ProductPhase.DELETED;
        };
    }
}
