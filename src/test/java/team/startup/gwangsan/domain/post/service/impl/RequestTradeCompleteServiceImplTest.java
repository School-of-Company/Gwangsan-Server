package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RequestTradeCompleteServiceImpl 단위 테스트")
class RequestTradeCompleteServiceImplTest {

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
        @DisplayName("구매자가 예약 상태 상품에 대해 거래 완료 요청 시 거래를 완료 처리한다")
        void execute_asBuyer_withReservation_shouldCompleteTrade() {
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
            when(product.getGwangsan()).thenReturn(10);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ProductReservation reservation = mock(ProductReservation.class);
            when(productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(reservation));
            when(reservation.getReserver()).thenReturn(buyerMember);

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(true);

            TradeComplete pending = mock(TradeComplete.class);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            when(deviceTokenRepository.findByUserId(buyerId))
                    .thenReturn(Optional.of(mock(DeviceToken.class)));
            when(deviceTokenRepository.findByUserId(sellerId))
                    .thenReturn(Optional.empty());

            // when
            assertDoesNotThrow(() -> service.execute(productId, sellerId));

            // then
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(pending).updateStatus(TradeStatus.COMPLETED);
            verify(pending).updateCompletedAt();
            verify(reservation).complete();
        }

        @Test
        @DisplayName("판매자가 최초로 거래 완료 요청 시 PENDING 거래 완료 엔티티를 생성한다")
        void execute_asSeller_shouldCreatePendingTrade_whenNoExistingPending() {
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
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, sellerId))
                    .thenReturn(true);

            when(tradeCompleteRepository.existsByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(false);

            TradeComplete newTradeComplete = mock(TradeComplete.class);
            when(newTradeComplete.getId()).thenReturn(999L);
            when(newTradeComplete.getBuyer()).thenReturn(buyerMember);

            when(tradeCompleteRepository.save(any(TradeComplete.class)))
                    .thenReturn(newTradeComplete);

            // when
            assertDoesNotThrow(() -> service.execute(productId, buyerId));

            // then
            verify(tradeCompleteRepository).save(any(TradeComplete.class));
        }

        @Test
        @DisplayName("판매자가 이미 PENDING 거래 완료 요청이 있을 때 다시 요청하면 TradeAlreadyCompleteRequestException 을 던진다")
        void execute_asSeller_shouldThrowTradeAlreadyCompleteRequestException_whenPendingExists() {
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
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, sellerId))
                    .thenReturn(true);

            when(tradeCompleteRepository.existsByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(true);

            // when & then
            assertThrows(TradeAlreadyCompleteRequestException.class,
                    () -> service.execute(productId, buyerId));

            verify(tradeCompleteRepository, never()).save(any());
        }

        @Test
        @DisplayName("예약이 없는 상품에 대해 구매자가 거래 완료 요청 시 예약 없이 거래를 완료 처리한다")
        void execute_asBuyer_withoutReservation_shouldCompleteTrade() {
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
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId,
                    buyerMember,
                    sellerMember
            )).thenReturn(Optional.of(chatRoom));

            when(chatMessageRepository.existsByRoomAndSenderId(chatRoom, buyerId))
                    .thenReturn(true);

            TradeComplete pending = mock(TradeComplete.class);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product,
                    buyerMember,
                    sellerMember,
                    TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            when(deviceTokenRepository.findByUserId(buyerId))
                    .thenReturn(Optional.of(mock(DeviceToken.class)));
            when(deviceTokenRepository.findByUserId(sellerId))
                    .thenReturn(Optional.empty());

            // when
            assertDoesNotThrow(() -> service.execute(productId, sellerId));

            // then
            verify(product).updateStatus(ProductStatus.COMPLETED);
            verify(pending).updateStatus(TradeStatus.COMPLETED);
            verify(pending).updateCompletedAt();
            verifyNoInteractions(productReservationRepository);
        }

        @Test
        @DisplayName("구매자가 거래 완료 요청 시 판매자의 PENDING 거래 완료 요청이 없으면 SellerNotTradeCompleteException 을 던진다")
        void execute_asBuyer_shouldThrowSellerNotTradeCompleteException_whenNoPendingTradeFromSeller() {
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
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);

            Member productOwner = mock(Member.class);
            when(product.getMember()).thenReturn(productOwner);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
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

            // when & then
            assertThrows(SellerNotTradeCompleteException.class,
                    () -> service.execute(productId, sellerId));

            verify(product, never()).updateStatus(any());
        }
    }

}