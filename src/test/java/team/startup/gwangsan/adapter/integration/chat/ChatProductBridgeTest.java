package team.startup.gwangsan.adapter.integration.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatTradeCard;
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
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatProductBridge (post·trade 공개 Port) 단위 테스트")
class ChatProductBridgeTest {

    private static final Long PRODUCT = 10L;
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 30, 11, 20);

    @Mock private ProductSnapshotQuery productSnapshotQuery;
    @Mock private ProductImageQuery productImageQuery;
    @Mock private PendingReservationUseCase pendingReservationUseCase;
    @Mock private TradeStateQuery tradeStateQuery;
    @Mock private CompletedTradeQuery completedTradeQuery;
    @InjectMocks private ChatProductBridge bridge;

    private static ProductSnapshot product(Long id, ProductStatus status, Mode mode) {
        return new ProductSnapshot(id, 2L, "상품명", "설명", 5000, Type.SERVICE, mode, status);
    }

    @BeforeEach
    void setUp() {
        when(productSnapshotQuery.get(PRODUCT)).thenReturn(product(PRODUCT, ProductStatus.ONGOING, Mode.GIVER));
        when(productImageQuery.findImages(PRODUCT)).thenReturn(List.of());
    }

    private void givenState(boolean completed, boolean reserved, Boolean requestedBySeller, LocalDateTime requestedAt) {
        when(tradeStateQuery.query(any())).thenReturn(new TradeStateSnapshot(completed, reserved, requestedBySeller, requestedAt));
    }

    @Nested
    @DisplayName("거래 카드는")
    class Describe_trade_card {

        @ParameterizedTest
        @CsvSource(value = {"false, null, true, true", "false, true, false, true", "false, false, true, false",
                "true, null, false, false"}, nullValues = "null")
        @DisplayName("it_역할별 완료 가능 여부를 거래 스냅샷 규칙으로 계산한다")
        void it_computes_completable_per_role(boolean completed, Boolean requestedBySeller,
                                              boolean bySeller, boolean byBuyer) {
            givenState(completed, false, requestedBySeller, REQUESTED_AT);

            ChatTradeCard card = bridge.loadTradeCard(PRODUCT, 1L, 2L);

            assertThat(card.completableBySeller()).isEqualTo(bySeller);
            assertThat(card.completableByBuyer()).isEqualTo(byBuyer);
            assertThat(card.requestedAt()).isEqualTo(REQUESTED_AT);
            assertThat(card.ownerId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("it_거래 상태는 방의 상품·구매자·판매자 키로 상품 → 이미지 다음에 조회한다")
        void it_queries_state_with_room_key_in_order() {
            givenState(false, false, null, null);

            bridge.loadTradeCard(PRODUCT, 1L, 2L);

            InOrder order = inOrder(productSnapshotQuery, productImageQuery, tradeStateQuery);
            order.verify(productSnapshotQuery).get(PRODUCT);
            order.verify(productImageQuery).findImages(PRODUCT);
            order.verify(tradeStateQuery).query(new TradeStateKey(PRODUCT, 1L, 2L));
        }

        @Test
        @DisplayName("it_예약 중이 아니면 예약을 조회하지 않고, 완료가 아니면 완료 시각을 조회하지 않는다")
        void it_skips_reservation_and_completion_queries() {
            givenState(false, false, null, null);

            ChatTradeCard card = bridge.loadTradeCard(PRODUCT, 1L, 2L);

            assertThat(card.reservation()).isNull();
            assertThat(card.completedAt()).isNull();
            assertThat(card.requestedAt()).isNull();
            verifyNoInteractions(pendingReservationUseCase, completedTradeQuery);
        }

        @Test
        @DisplayName("it_예약 중이면 진행 중 예약의 일정·장소를 담는다")
        void it_loads_pending_reservation() {
            givenState(false, true, null, REQUESTED_AT);
            when(pendingReservationUseCase.findPending(PRODUCT)).thenReturn(Optional.of(new PendingReservationView(
                    5L, PRODUCT, 1L, REQUESTED_AT.plusDays(1), REQUESTED_AT, "광산구청", "주소", 35.1, 126.7)));

            ChatTradeCard card = bridge.loadTradeCard(PRODUCT, 1L, 2L);

            assertThat(card.reserved()).isTrue();
            assertThat(card.reservation()).isEqualTo(new ChatTradeCard.Reservation(REQUESTED_AT.plusDays(1), REQUESTED_AT,
                    "광산구청", "주소", 35.1, 126.7));
        }

        @Test
        @DisplayName("it_예약 상태라도 진행 중 예약이 없으면 예약 정보는 null 이다")
        void it_handles_missing_pending_reservation() {
            givenState(false, true, null, REQUESTED_AT);
            when(pendingReservationUseCase.findPending(PRODUCT)).thenReturn(Optional.empty());

            assertThat(bridge.loadTradeCard(PRODUCT, 1L, 2L).reservation()).isNull();
        }

        @Test
        @DisplayName("it_완료 후에도 요청 시각을 유지하고 완료 시각을 담는다")
        void it_keeps_requested_at_after_completion() {
            givenState(true, false, null, REQUESTED_AT);
            when(completedTradeQuery.query(PRODUCT))
                    .thenReturn(Optional.of(new CompletedTrade(7L, 1L, 2L, REQUESTED_AT.plusDays(2))));

            ChatTradeCard card = bridge.loadTradeCard(PRODUCT, 1L, 2L);

            assertThat(card.requestedAt()).isEqualTo(REQUESTED_AT);
            assertThat(card.completed()).isTrue();
            assertThat(card.completedAt()).isEqualTo(REQUESTED_AT.plusDays(2));
        }

        @Test
        @DisplayName("it_완료 기록이 없으면 완료 시각은 null 이다")
        void it_handles_missing_completion() {
            givenState(true, false, null, REQUESTED_AT);
            when(completedTradeQuery.query(anyLong())).thenReturn(Optional.empty());

            assertThat(bridge.loadTradeCard(PRODUCT, 1L, 2L).completedAt()).isNull();
        }

        @Test
        @DisplayName("it_삭제 상품과 상품 이미지를 그대로 옮긴다")
        void it_maps_deleted_and_images() {
            givenState(false, false, null, null);
            when(productSnapshotQuery.get(PRODUCT)).thenReturn(product(PRODUCT, ProductStatus.DELETED, Mode.GIVER));
            when(productImageQuery.findImages(PRODUCT)).thenReturn(List.of(new ImageView(3L, "u3")));

            ChatTradeCard card = bridge.loadTradeCard(PRODUCT, 1L, 2L);

            assertThat(card.deleted()).isTrue();
            assertThat(card.images()).containsExactly(new ChatImage(3L, "u3"));
        }

        @Test
        @DisplayName("it_상품이 없으면 post 의 NotFoundProductException 이 그대로 나간다")
        void it_propagates_not_found_product() {
            when(productSnapshotQuery.get(PRODUCT)).thenThrow(new NotFoundProductException());

            assertThatThrownBy(() -> bridge.loadTradeCard(PRODUCT, 1L, 2L)).isInstanceOf(NotFoundProductException.class);
            verifyNoInteractions(tradeStateQuery);
        }
    }

    @Nested
    @DisplayName("상품 조회는")
    class Describe_products {

        @Test
        @DisplayName("it_활성 상품이 없으면 기존 NotFoundProductException 을 던진다")
        void it_throws_not_found_product() {
            when(productSnapshotQuery.getActive(PRODUCT)).thenThrow(new NotFoundProductException());

            assertThatThrownBy(() -> bridge.loadActive(PRODUCT)).isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("it_GIVER 여부와 작성자 ID 를 돌려준다")
        void it_maps_active_product() {
            when(productSnapshotQuery.getActive(PRODUCT)).thenReturn(product(PRODUCT, ProductStatus.ONGOING, Mode.RECEIVER));

            var active = bridge.loadActive(PRODUCT);

            assertThat(active.productId()).isEqualTo(PRODUCT);
            assertThat(active.ownerId()).isEqualTo(2L);
            assertThat(active.giver()).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
                "COMPLETED, true, false, false",
                "RESERVATION, false, true, false",
                "ONGOING, false, false, false",
                "DELETED, false, false, true"
        })
        @DisplayName("it_방 목록 상품 상태를 isCompleted/isReserved/isDeleted 로 매핑한다 (삭제 상품 포함)")
        void it_maps_room_product_status(ProductStatus status, boolean completed, boolean reserved, boolean deleted) {
            when(productSnapshotQuery.findAll(Set.of(PRODUCT))).thenReturn(Map.of(PRODUCT, product(PRODUCT, status, Mode.GIVER)));
            when(productImageQuery.findImages(Set.of(PRODUCT))).thenReturn(Map.of(PRODUCT, List.of(new ImageView(3L, "u3"))));

            assertThat(bridge.findRoomProducts(Set.of(PRODUCT))).containsExactlyEntriesOf(Map.of(PRODUCT,
                    new ChatRoomProduct(PRODUCT, "상품명", completed, reserved, deleted, List.of(new ChatImage(3L, "u3")))));
        }

        @Test
        @DisplayName("it_이미지 없는 상품은 빈 이미지 목록, 조회되지 않는 상품은 결과에서 빠진다")
        void it_handles_missing_images_and_products() {
            when(productSnapshotQuery.findAll(Set.of(PRODUCT, 99L))).thenReturn(Map.of(PRODUCT, product(PRODUCT, ProductStatus.ONGOING, Mode.GIVER)));
            when(productImageQuery.findImages(Set.of(PRODUCT))).thenReturn(Map.of());

            assertThat(bridge.findRoomProducts(Set.of(PRODUCT, 99L))).containsExactlyEntriesOf(Map.of(PRODUCT,
                    new ChatRoomProduct(PRODUCT, "상품명", false, false, false, List.of())));
        }
    }
}
