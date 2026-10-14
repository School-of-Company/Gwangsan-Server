package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.ChatProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatTradeCard;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.PendingReservationView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.PendingReservationUseCase;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.trade.data.CompletedTrade;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.port.in.CompletedTradeQuery;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;
import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * chat → post(PR06)·trade(PR07) 공개 Port. 상품 부재는 post 의 NotFoundProductException(404) 이 그대로 나간다.
 */
@Component
@RequiredArgsConstructor
public class ChatProductBridge implements ChatProductPort {

    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;
    private final PendingReservationUseCase pendingReservationUseCase;
    private final TradeStateQuery tradeStateQuery;
    private final CompletedTradeQuery completedTradeQuery;

    @Override
    public ChatProduct loadActive(Long productId) {
        ProductSnapshot product = productSnapshotQuery.getActive(productId);
        return new ChatProduct(product.id(), product.authorId(), product.isGiver());
    }

    /** 삭제 상품 포함. 상태 값은 trade TradeState 의 completed/reserved 와 같은 기준(상품 상태)이다. */
    @Override
    public Map<Long, ChatRoomProduct> findRoomProducts(Set<Long> productIds) {
        Map<Long, ProductSnapshot> products = productSnapshotQuery.findAll(productIds);
        Map<Long, List<ImageView>> images = productImageQuery.findImages(products.keySet());
        return products.values().stream().collect(Collectors.toMap(ProductSnapshot::id, product -> new ChatRoomProduct(
                product.id(), product.title(), product.isCompleted(), product.isReserved(), product.isDeleted(),
                images(images.getOrDefault(product.id(), List.of())))));
    }

    /** 조회 순서(상품 → 상품 이미지 → 거래 상태 → 예약 → 완료 시각)는 기존 채팅방 조회와 같다. */
    @Override
    public ChatTradeCard loadTradeCard(Long productId, Long buyerId, Long sellerId) {
        ProductSnapshot product = productSnapshotQuery.get(productId);
        List<ChatImage> images = images(productImageQuery.findImages(productId));
        // 거래 상태 변경 전달과 같은 값을 쓰기 위해 조회도 이 스냅샷을 거친다.
        TradeStateSnapshot state = tradeStateQuery.query(new TradeStateKey(productId, buyerId, sellerId));
        Optional<PendingReservationView> reservation = state.reserved()
                ? pendingReservationUseCase.findPending(productId)
                : Optional.empty();
        LocalDateTime completedAt = state.completed()
                ? completedTradeQuery.query(productId).map(CompletedTrade::completedAt).orElse(null)
                : null;
        return new ChatTradeCard(
                product.id(),
                product.title(),
                images,
                product.authorId(),
                product.isDeleted(),
                state.completed(),
                state.reserved(),
                state.isCompletableFor(true),
                state.isCompletableFor(false),
                state.requestedAt(),
                reservation.map(r -> new ChatTradeCard.Reservation(r.scheduledAt(), r.createdAt(),
                        r.placeName(), r.address(), r.latitude(), r.longitude())).orElse(null),
                completedAt);
    }

    private static List<ChatImage> images(List<ImageView> images) {
        return images.stream().map(image -> new ChatImage(image.imageId(), image.imageUrl())).toList();
    }
}
