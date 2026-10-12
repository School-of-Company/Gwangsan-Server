package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.trade.exception.*;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RequestTradeCompleteServiceImpl 단위 테스트")
class RequestTradeCompleteServiceImplTest {

    private static final LocalDateTime TRADE_REQUESTED_AT = LocalDateTime.of(2026, 8, 30, 11, 20);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private TradeCompleteRepository tradeCompleteRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ProductReservationRepository productReservationRepository;

    @InjectMocks
    private RequestTradeCompleteServiceImpl service;

    private static final String PHONE_NUMBER = "010-1111-2222";

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(PHONE_NUMBER);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MemberDetail mockMemberDetail(Long memberId) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);

        MemberDetail detail = mock(MemberDetail.class);
        when(detail.getMember()).thenReturn(member);
        when(detail.getId()).thenReturn(memberId);
        return detail;
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("본인과 거래를 시도하면 CannotSelectSelfException 을 던진다")
        void execute_shouldThrowCannotSelectSelfException_whenSelfTrade() {
            // given
            Long memberId = 1L;
            MemberDetail memberDetail = mockMemberDetail(memberId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(memberDetail);

            // when & then
            assertThrows(CannotSelectSelfException.class,
                    () -> service.execute(10L, memberId));

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException을 던진다")
        void execute_shouldThrowNotFoundProductException_whenProductNotFound() {
            Long memberId = 1L;
            Long otherMemberId = 2L;

            MemberDetail memberDetail = mockMemberDetail(memberId);
            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER)).thenReturn(memberDetail);
            when(productRepository.findByIdWithLock(100L)).thenReturn(Optional.empty());

            assertThrows(NotFoundProductException.class, () -> service.execute(100L, otherMemberId));
        }

        @Test
        @DisplayName("이미 거래 완료된 상품이면 TradeAlreadyCompleteException 을 던진다")
        void execute_shouldThrowTradeAlreadyCompleteException_whenProductAlreadyCompleted() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;

            MemberDetail memberDetail = mockMemberDetail(memberId);
            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(memberDetail);

            Product product = mock(Product.class);
            when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
            when(productRepository.findByIdWithLock(100L))
                    .thenReturn(Optional.of(product));

            // when & then
            assertThrows(TradeAlreadyCompleteException.class,
                    () -> service.execute(100L, otherMemberId));

            verify(productRepository).findByIdWithLock(100L);
        }

        @Test
        @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던진다")
        void execute_shouldThrowNotFoundChatRoomException_whenChatRoomNotFound() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerDetail.getMember(),
                    sellerDetail.getMember()
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundChatRoomException.class,
                    () -> service.execute(productId, sellerId));
        }

        @Test
        @DisplayName("채팅 내역이 없으면 TradeCompleteWithoutChattingException 을 던진다")
        void execute_shouldThrowTradeCompleteWithoutChattingException_whenNoChatMessages() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerDetail.getMember(),
                    sellerDetail.getMember()
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(false);

            // when & then
            assertThrows(TradeCompleteWithoutChattingException.class,
                    () -> service.execute(productId, sellerId));
        }

        @Test
        @DisplayName("예약 상태 상품에서 상품 등록자가 아닌 사용자가, 예약자가 아닌 상대와 거래 완료를 시도하면 ReservationParticipantOnlyException 을 던진다")
        void execute_shouldThrowReservationParticipantOnlyException_whenReserverIsNotBuyerOrSeller() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            Member buyerMember = buyerDetail.getMember();
            Member sellerMember = sellerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);
            when(product.getMode()).thenReturn(Mode.GIVER);

            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ProductReservation reservation = mock(ProductReservation.class);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(reservation));

            Member thirdMember = mock(Member.class);
            when(reservation.getReserver()).thenReturn(thirdMember);

            // when & then
            assertThrows(ReservationParticipantOnlyException.class,
                    () -> service.execute(productId, sellerId));
        }

        @Test
        @DisplayName("판매자가 이미 요청을 보낸 상태에서 구매자가 예약 상품에 대해 확정하면 거래를 완료 처리한다")
        void execute_asBuyer_withReservation_shouldCompleteTrade() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;
            Long roomId = 700L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            Member buyerMember = buyerDetail.getMember();
            Member sellerMember = sellerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);
            when(product.getMode()).thenReturn(Mode.GIVER);

            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);
            when(product.getGwangsan()).thenReturn(10);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ProductReservation reservation = mock(ProductReservation.class);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(reservation));
            when(reservation.getReserver()).thenReturn(buyerMember);

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(true);

            // 판매자가 먼저 요청을 보낸 상태(requestedBySeller = true)
            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(true);
            when(pending.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            when(deviceTokenRepository.findAllByUserId(buyerId))
                    .thenReturn(List.of(mock(DeviceToken.class)));
            when(deviceTokenRepository.findAllByUserId(sellerId))
                    .thenReturn(List.of());

            // when
            assertDoesNotThrow(() -> service.execute(productId, sellerId));

            // then
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(pending).updateStatus(TradeStatus.COMPLETED);
            verify(pending).updateCompletedAt();
            verify(reservation).complete();
            verifyTradeStatusChangedEvent(roomId, null, productId, true, false);
        }

        @Test
        @DisplayName("작성자인 판매자가 최초 호출하면 즉시 완료한다")
        void execute_asSeller_shouldCompleteTrade_whenNoExistingPending() {
            // given
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;
            Long roomId = 701L;

            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            MemberDetail buyerDetail = mockMemberDetail(buyerId);

            Member sellerMember = sellerDetail.getMember();
            Member buyerMember = buyerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(sellerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(buyerId))
                    .thenReturn(buyerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getMember()).thenReturn(sellerMember);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, sellerId))
                    .thenReturn(true);

            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.empty());

            TradeComplete newTradeComplete = mock(TradeComplete.class);
            when(newTradeComplete.getId()).thenReturn(999L);
            // 이벤트가 조회 응답과 같은 값을 싣는지 확인하기 위한 스텁
            when(newTradeComplete.isRequestedBySeller()).thenReturn(true);
            when(newTradeComplete.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);

            when(tradeCompleteRepository.save(any(TradeComplete.class)))
                    .thenReturn(newTradeComplete);

            // when
            assertDoesNotThrow(() -> service.execute(productId, buyerId));

            // then
            ArgumentCaptor<TradeComplete> savedCaptor = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompleteRepository).save(savedCaptor.capture());
            assertTrue(savedCaptor.getValue().isRequestedBySeller());
            assertEquals(TradeStatus.COMPLETED, savedCaptor.getValue().getStatus());
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(newTradeComplete).updateCompletedAt();
            verifyTradeStatusChangedEvent(roomId, null, productId, true, false);
        }

        @Test
        @DisplayName("구매자가 최초로 거래 완료를 요청하면 PENDING 거래 완료 엔티티를 생성한다")
        void execute_asBuyer_shouldCreatePendingTrade_whenNoExistingPending() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;
            Long roomId = 703L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            Member buyerMember = buyerDetail.getMember();
            Member sellerMember = sellerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);

            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(true);

            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.empty());

            TradeComplete newTradeComplete = mock(TradeComplete.class);
            when(newTradeComplete.getId()).thenReturn(998L);
            when(newTradeComplete.isRequestedBySeller()).thenReturn(false);
            when(newTradeComplete.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);

            when(tradeCompleteRepository.save(any(TradeComplete.class)))
                    .thenReturn(newTradeComplete);

            // when
            assertDoesNotThrow(() -> service.execute(productId, sellerId));

            // then
            ArgumentCaptor<TradeComplete> savedCaptor = ArgumentCaptor.forClass(TradeComplete.class);
            verify(tradeCompleteRepository).save(savedCaptor.capture());
            assertTrue(!savedCaptor.getValue().isRequestedBySeller());
            verifyTradeStatusChangedEvent(roomId, false, productId, false, false);

            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyerMember, sellerMember, TradeStatus.PENDING))
                    .thenReturn(Optional.of(newTradeComplete));
            assertThrows(TradeAlreadyCompleteRequestException.class,
                    () -> service.execute(productId, sellerId));
            verify(product, never()).updateStatus(any());
            verify(tradeCompleteRepository, times(1)).save(any());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("작성자인 판매자는 요청자가 누구든 PENDING을 완료한다")
        void execute_asAuthor_shouldCompletePending(boolean requestedBySeller) {
            // given
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;

            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            MemberDetail buyerDetail = mockMemberDetail(buyerId);

            Member sellerMember = sellerDetail.getMember();
            Member buyerMember = buyerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(sellerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(buyerId))
                    .thenReturn(buyerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getMember()).thenReturn(sellerMember);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(300L);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, sellerId))
                    .thenReturn(true);

            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(requestedBySeller);
            when(pending.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            // when & then
            assertDoesNotThrow(() -> service.execute(productId, buyerId));

            verify(tradeCompleteRepository, never()).save(any());
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(pending).updateStatus(TradeStatus.COMPLETED);
            verify(pending).updateCompletedAt();
            verifyTradeStatusChangedEvent(300L, null, productId, true, false);
        }

        @Test
        @DisplayName("예약이 없는 상품에 대해 구매자가 확정하면 예약 없이 거래를 완료 처리한다")
        void execute_asBuyer_withoutReservation_shouldCompleteTrade() {
            // given
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;
            Long roomId = 702L;

            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);

            Member buyerMember = buyerDetail.getMember();
            Member sellerMember = sellerDetail.getMember();

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId))
                    .thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);

            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);
            when(product.getGwangsan()).thenReturn(10);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            when(productReservationRepository.findByProductAndStatus(any(), any()))
                    .thenReturn(Optional.empty());

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(true);

            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(true);
            when(pending.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            when(deviceTokenRepository.findAllByUserId(buyerId))
                    .thenReturn(List.of(mock(DeviceToken.class)));
            when(deviceTokenRepository.findAllByUserId(sellerId))
                    .thenReturn(List.of());

            // when
            assertDoesNotThrow(() -> service.execute(productId, sellerId));

            // then
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(pending).updateStatus(TradeStatus.COMPLETED);
            verify(pending).updateCompletedAt();
            verifyNoInteractions(productReservationRepository);
            verifyTradeStatusChangedEvent(roomId, null, productId, true, false);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("RECEIVER 작성자는 구매자로 즉시 완료하고 광산을 판매자에게 이동한다")
        void execute_as_receiver_author_completes_trade(boolean withOwnPending) {
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long productId = 100L;
            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            Member buyer = buyerDetail.getMember();
            Member seller = sellerDetail.getMember();
            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER)).thenReturn(buyerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(sellerId)).thenReturn(sellerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.RECEIVER);
            when(product.getMember()).thenReturn(buyer);
            when(product.getGwangsan()).thenReturn(10);
            when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(20L);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(productId, buyer, seller))
                    .thenReturn(Optional.of(chatRoom));
            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId)).thenReturn(true);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyer, seller, TradeStatus.PENDING)).thenReturn(Optional.empty());
            TradeComplete saved = mock(TradeComplete.class);
            when(saved.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);
            when(tradeCompleteRepository.save(any(TradeComplete.class))).thenReturn(saved);
            if (withOwnPending) {
                when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                        product, buyer, seller, TradeStatus.PENDING)).thenReturn(Optional.of(saved));
            }

            service.execute(productId, sellerId);

            if (withOwnPending) {
                verify(tradeCompleteRepository, never()).save(any());
            } else {
                ArgumentCaptor<TradeComplete> tradeCaptor = ArgumentCaptor.forClass(TradeComplete.class);
                verify(tradeCompleteRepository).save(tradeCaptor.capture());
                assertEquals(buyer, tradeCaptor.getValue().getBuyer());
                assertEquals(seller, tradeCaptor.getValue().getSeller());
                assertEquals(false, tradeCaptor.getValue().isRequestedBySeller());
                assertEquals(TradeStatus.COMPLETED, tradeCaptor.getValue().getStatus());
            }
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(saved).updateStatus(TradeStatus.COMPLETED);
            verify(buyerDetail).minusGwangsan(10);
            verify(sellerDetail).plusGwangsan(10);
            verify(saved).updateCompletedAt();
            verifyTradeStatusChangedEvent(20L, null, productId, true, false);
        }

        @Test
        @DisplayName("예약자가 판매자인 작성자도 즉시 완료하고 예약을 완료한다")
        void execute_as_reserved_seller_completes_trade() {
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;
            Long roomId = 30L;
            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            MemberDetail buyerDetail = mockMemberDetail(buyerId);
            Member seller = sellerDetail.getMember();
            Member buyer = buyerDetail.getMember();
            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER)).thenReturn(sellerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(buyerId)).thenReturn(buyerDetail);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getMember()).thenReturn(seller);
            when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
            ProductReservation reservation = mock(ProductReservation.class);
            when(reservation.getReserver()).thenReturn(seller);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(reservation));
            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(productId, buyer, seller))
                    .thenReturn(Optional.of(chatRoom));
            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, sellerId)).thenReturn(true);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyer, seller, TradeStatus.PENDING)).thenReturn(Optional.empty());
            TradeComplete saved = mock(TradeComplete.class);
            when(saved.getId()).thenReturn(55L);
            when(saved.isRequestedBySeller()).thenReturn(true);
            when(saved.getCreatedAt()).thenReturn(TRADE_REQUESTED_AT);
            when(tradeCompleteRepository.save(any(TradeComplete.class))).thenReturn(saved);

            service.execute(productId, buyerId);

            verify(reservation).complete();
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(saved).updateStatus(TradeStatus.COMPLETED);
            verify(saved).updateCompletedAt();
            verifyTradeStatusChangedEvent(roomId, null, productId, true, false);
        }
    }

    /**
     * @param requestedBySeller 대기 중인 요청을 만든 쪽이 판매자인지. 확정 직후처럼
     *                          대기 중인 요청이 없으면 null 이어야 한다.
     */
    private void verifyTradeStatusChangedEvent(
            Long roomId,
            Boolean requestedBySeller,
            Long productId,
            boolean completed,
            boolean reserved
    ) {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        TradeStatusChangedEvent event = eventCaptor.getAllValues().stream()
                .filter(TradeStatusChangedEvent.class::isInstance)
                .map(TradeStatusChangedEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertEquals(roomId, event.roomId());
        assertEquals(requestedBySeller, event.requestedBySeller());
        assertEquals(productId, event.productId());
        assertEquals(completed, event.completed());
        assertEquals(reserved, event.reserved());
        // 조회 응답과 같은 값이어야 하므로 거래 요청 시각이 실려 있어야 한다.
        assertNotNull(event.requestedAt());
        if (completed) {
            SendNotificationEvent notification = eventCaptor.getAllValues().stream()
                    .filter(SendNotificationEvent.class::isInstance)
                    .map(SendNotificationEvent.class::cast)
                    .findFirst().orElseThrow();
            assertEquals(NotificationType.TRADE_COMPLETE, notification.type());
            assertEquals(productId, notification.sourceId());
            verify(tradeCompleteRepository).deleteByProductAndStatus(any(Product.class), eq(TradeStatus.PENDING));
        }
    }
}
