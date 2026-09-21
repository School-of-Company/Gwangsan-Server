package team.startup.gwangsan.domain.trade.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeCancelApplier 단위 테스트")
class TradeCancelApplierTest {

    private static final Long TRADE_CANCEL_ID = 5L;
    private static final Long PRODUCT_ID = 7L;
    private static final Long CHAT_ROOM_ID = 77L;
    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 3L;
    private static final Long REQUESTER_ID = 2L;
    private static final int GWANGSAN = 1000;

    @InjectMocks private TradeCancelApplier applier;

    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private TradeStateReader tradeStateReader;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private Member buyer;
    private Member seller;
    private Product product;
    private TradeComplete tradeComplete;
    private MemberDetail buyerDetail;
    private MemberDetail sellerDetail;

    private Member member(Long id) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        return member;
    }

    /** PENDING 인 철회 요청과 그 주변 엔티티를 준비한다. */
    private TradeCancel givenPendingTradeCancel() {
        buyer = member(BUYER_ID);
        seller = member(SELLER_ID);

        product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(PRODUCT_ID);
        lenient().when(product.getGwangsan()).thenReturn(GWANGSAN);

        tradeComplete = mock(TradeComplete.class);
        lenient().when(tradeComplete.getProduct()).thenReturn(product);
        lenient().when(tradeComplete.getBuyer()).thenReturn(buyer);
        lenient().when(tradeComplete.getSeller()).thenReturn(seller);

        Member requester = member(REQUESTER_ID);

        TradeCancel tradeCancel = mock(TradeCancel.class);
        lenient().when(tradeCancel.getId()).thenReturn(TRADE_CANCEL_ID);
        lenient().when(tradeCancel.getStatus()).thenReturn(TradeCancelStatus.PENDING);
        lenient().when(tradeCancel.getTradeComplete()).thenReturn(tradeComplete);
        lenient().when(tradeCancel.getMember()).thenReturn(requester);

        buyerDetail = mock(MemberDetail.class);
        sellerDetail = mock(MemberDetail.class);
        lenient().when(memberDetailRepository.findById(BUYER_ID)).thenReturn(Optional.of(buyerDetail));
        lenient().when(memberDetailRepository.findById(SELLER_ID)).thenReturn(Optional.of(sellerDetail));

        return tradeCancel;
    }

    private void givenNoAdminAlert() {
        lenient().when(adminAlertRepository.findByTypeAndSourceId(AlertType.TRADE_CANCEL, TRADE_CANCEL_ID))
                .thenReturn(Optional.empty());
    }

    private void givenNoChatRoom() {
        lenient().when(chatRoomRepository.findByProductIdAndBuyerAndSeller(PRODUCT_ID, buyer, seller))
                .thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("apply() 메서드는")
    class Describe_apply {

        @Nested
        @DisplayName("PENDING 상태의 철회 요청을 받으면")
        class Context_with_pending_trade_cancel {

            @Test
            @DisplayName("실제 구매자·판매자 잔액과 거래 상태를 되돌린다")
            void it_moves_gwangsan_and_rolls_back_actual_entities() {
                TradeCancel tradeCancel = givenActualPendingTradeCancel();
                givenNoAdminAlert();
                givenNoChatRoom();

                applier.apply(tradeCancel);

                assertThat(buyerDetail.getGwangsan()).isEqualTo(6_000);
                assertThat(sellerDetail.getGwangsan()).isEqualTo(3_000);
                assertThat(tradeCancel.getStatus()).isEqualTo(TradeCancelStatus.APPROVED);
                assertThat(tradeComplete.getStatus()).isEqualTo(TradeStatus.ROLLED_BACK);
                assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
            }

            @Test
            @DisplayName("철회 완료 알림 이벤트를 발행한다")
            void it_publishes_create_alert_event() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                givenNoAdminAlert();
                givenNoChatRoom();

                applier.apply(tradeCancel);

                ArgumentCaptor<CreateAlertEvent> captor = ArgumentCaptor.forClass(CreateAlertEvent.class);
                verify(applicationEventPublisher).publishEvent(captor.capture());

                CreateAlertEvent event = captor.getValue();
                assertThat(event.sourceId()).isEqualTo(TRADE_CANCEL_ID);
                assertThat(event.memberId()).isEqualTo(REQUESTER_ID);
                assertThat(event.alertType())
                        .isEqualTo(team.startup.gwangsan.domain.alert.entity.constant.AlertType.TRADE_CANCEL);
            }

            @Test
            @DisplayName("남아 있는 관리자 알림을 지운다")
            void it_deletes_admin_alert() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                givenNoChatRoom();

                AdminAlert alert = mock(AdminAlert.class);
                when(adminAlertRepository.findByTypeAndSourceId(AlertType.TRADE_CANCEL, TRADE_CANCEL_ID))
                        .thenReturn(Optional.of(alert));

                applier.apply(tradeCancel);

                verify(adminAlertRepository).delete(alert);
            }

            @Test
            @DisplayName("관리자 알림이 이미 없으면 삭제를 시도하지 않는다")
            void it_skips_delete_when_admin_alert_absent() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                givenNoAdminAlert();
                givenNoChatRoom();

                applier.apply(tradeCancel);

                verify(adminAlertRepository, never()).delete(any());
            }
        }

        @Nested
        @DisplayName("채팅방이 있을 때")
        class Context_with_chat_room {

            @Test
            @DisplayName("대기 중인 요청이 없는 상태로 거래 상태 이벤트를 발행한다")
            void it_publishes_trade_status_changed_event() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                givenNoAdminAlert();

                ChatRoom chatRoom = mock(ChatRoom.class);
                when(chatRoom.getId()).thenReturn(CHAT_ROOM_ID);
                when(chatRoomRepository.findByProductIdAndBuyerAndSeller(PRODUCT_ID, buyer, seller))
                        .thenReturn(Optional.of(chatRoom));
                // 철회가 반영되면 대기 중인 요청도 완료된 요청도 남지 않는다.
                when(tradeStateReader.read(product, buyer, seller))
                        .thenReturn(new TradeStateSnapshot(false, false, null, null));

                applier.apply(tradeCancel);

                // 알림 이벤트와 거래 상태 이벤트가 함께 나가므로 타입으로 골라낸다.
                ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());

                TradeStatusChangedEvent event = captor.getAllValues().stream()
                        .filter(TradeStatusChangedEvent.class::isInstance)
                        .map(TradeStatusChangedEvent.class::cast)
                        .findFirst()
                        .orElseThrow();

                assertThat(event.roomId()).isEqualTo(CHAT_ROOM_ID);
                assertThat(event.productId()).isEqualTo(PRODUCT_ID);
                assertFalse(event.completed());
                assertFalse(event.reserved());
                // 값이 남으면 클라이언트가 아직 거래 요청이 있다고 보고 재요청 버튼을 잠근다.
                assertNull(event.requestedBySeller());
                assertNull(event.requestedAt());
            }
        }

        @Nested
        @DisplayName("채팅방이 없을 때")
        class Context_without_chat_room {

            @Test
            @DisplayName("거래 상태 이벤트를 발행하지 않는다")
            void it_does_not_publish_trade_status_changed_event() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                givenNoAdminAlert();
                givenNoChatRoom();

                applier.apply(tradeCancel);

                verify(applicationEventPublisher, never()).publishEvent(any(TradeStatusChangedEvent.class));
                verify(tradeStateReader, never()).read(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("PENDING 상태가 아닐 때")
        class Context_with_non_pending_trade_cancel {

            @Test
            @DisplayName("CannotPendingTradeCancelException을 던지고 아무것도 바꾸지 않는다")
            void it_throws_and_changes_nothing() {
                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getStatus()).thenReturn(TradeCancelStatus.APPROVED);

                assertThatThrownBy(() -> applier.apply(tradeCancel))
                        .isInstanceOf(CannotPendingTradeCancelException.class);

                verifyNoInteractions(memberDetailRepository, adminAlertRepository,
                        chatRoomRepository, tradeStateReader, applicationEventPublisher);
            }
        }

        @Nested
        @DisplayName("회원 상세 정보가 없을 때")
        class Context_with_member_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberDetailException을 던진다")
            void it_throws_not_found_member_detail_exception() {
                TradeCancel tradeCancel = givenPendingTradeCancel();
                when(memberDetailRepository.findById(BUYER_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> applier.apply(tradeCancel))
                        .isInstanceOf(NotFoundMemberDetailException.class);

                verify(applicationEventPublisher, never()).publishEvent(any());
            }
        }
    }

    private TradeCancel givenActualPendingTradeCancel() {
        buyer = actualMember(BUYER_ID, "구매자");
        seller = actualMember(SELLER_ID, "판매자");

        product = Product.builder()
                .title("상품").description("설명").gwangsan(GWANGSAN).member(seller)
                .type(team.startup.gwangsan.domain.post.entity.constant.Type.SERVICE)
                .mode(team.startup.gwangsan.domain.post.entity.constant.Mode.GIVER)
                .status(ProductStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        tradeComplete = TradeComplete.builder()
                .product(product).buyer(buyer).seller(seller)
                .status(TradeStatus.COMPLETED).requestedBySeller(false)
                .build();

        TradeCancel tradeCancel = TradeCancel.builder()
                .tradeComplete(tradeComplete).member(buyer).reason("사유")
                .status(TradeCancelStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(tradeCancel, "id", TRADE_CANCEL_ID);

        buyerDetail = MemberDetail.builder().gwangsan(5_000).light(0).build();
        sellerDetail = MemberDetail.builder().gwangsan(4_000).light(0).build();
        when(memberDetailRepository.findById(BUYER_ID)).thenReturn(Optional.of(buyerDetail));
        when(memberDetailRepository.findById(SELLER_ID)).thenReturn(Optional.of(sellerDetail));

        return tradeCancel;
    }

    private Member actualMember(Long id, String name) {
        Member member = Member.builder()
                .name(name).nickname(name).phoneNumber("010-0000-000" + id).password("pw")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
