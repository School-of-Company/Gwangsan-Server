package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.NotTradeCompleteRequesterException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WithdrawTradeCompleteServiceImpl 단위 테스트")
class WithdrawTradeCompleteServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private TradeCompleteRepository tradeCompleteRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private TradeStateReader tradeStateReader;

    @InjectMocks
    private WithdrawTradeCompleteServiceImpl service;

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
            Long memberId = 1L;
            MemberDetail memberDetail = mockMemberDetail(memberId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(memberDetail);

            assertThrows(CannotSelectSelfException.class,
                    () -> service.execute(10L, memberId));

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException 을 던진다")
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
            Long memberId = 1L;
            Long otherMemberId = 2L;

            MemberDetail memberDetail = mockMemberDetail(memberId);
            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(memberDetail);

            Product product = mock(Product.class);
            when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);
            when(productRepository.findByIdWithLock(100L))
                    .thenReturn(Optional.of(product));

            assertThrows(TradeAlreadyCompleteException.class,
                    () -> service.execute(100L, otherMemberId));
        }

        @Test
        @DisplayName("대기 중인 거래 완료 요청이 없으면 NotFoundTradeCompleteException 을 던진다")
        void execute_shouldThrowNotFoundTradeCompleteException_whenNoPendingRequest() {
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
            when(product.getMember()).thenReturn(mock(Member.class));

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING
            )).thenReturn(Optional.empty());

            assertThrows(NotFoundTradeCompleteException.class,
                    () -> service.execute(productId, sellerId));

            verify(tradeCompleteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("요청자가 아닌 상대방이 철회를 시도하면 NotTradeCompleteRequesterException 을 던진다")
        void execute_shouldThrowNotTradeCompleteRequesterException_whenCallerIsNotRequester() {
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;

            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            MemberDetail buyerDetail = mockMemberDetail(buyerId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(sellerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(buyerId))
                    .thenReturn(buyerDetail);

            Member sellerMember = sellerDetail.getMember();

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getMember()).thenReturn(sellerMember);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            // 요청은 구매자가 보냈는데(requestedBySeller = false), 판매자(비요청자)가 철회를 시도
            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(false);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            assertThrows(NotTradeCompleteRequesterException.class,
                    () -> service.execute(productId, buyerId));

            verify(tradeCompleteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("채팅방이 없으면 NotFoundChatRoomException 을 던진다")
        void execute_shouldThrowNotFoundChatRoomException_whenChatRoomNotFound() {
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;

            MemberDetail sellerDetail = mockMemberDetail(sellerId);
            MemberDetail buyerDetail = mockMemberDetail(buyerId);

            when(memberDetailRepository.findByPhoneNumberWithMember(PHONE_NUMBER))
                    .thenReturn(sellerDetail);
            when(memberDetailRepository.findByMemberIdWithMember(buyerId))
                    .thenReturn(buyerDetail);

            Member sellerMember = sellerDetail.getMember();

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getMode()).thenReturn(Mode.GIVER);
            when(product.getMember()).thenReturn(sellerMember);

            when(productRepository.findByIdWithLock(productId))
                    .thenReturn(Optional.of(product));

            // 판매자 본인이 보낸 요청(requestedBySeller = true)을 판매자가 철회 시도
            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(true);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId, buyerDetail.getMember(), sellerDetail.getMember()
            )).thenReturn(Optional.empty());

            assertThrows(NotFoundChatRoomException.class,
                    () -> service.execute(productId, buyerId));

            verify(tradeCompleteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("요청자 본인이 철회하면 PENDING 요청을 삭제하고 최신 거래 상태로 이벤트를 발행한다")
        void execute_shouldDeletePendingAndPublishEvent_whenCallerIsRequester() {
            Long sellerId = 1L;
            Long buyerId = 2L;
            Long productId = 100L;
            Long roomId = 700L;

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

            TradeComplete pending = mock(TradeComplete.class);
            when(pending.isRequestedBySeller()).thenReturn(true);
            when(tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                    product, buyerMember, sellerMember, TradeStatus.PENDING
            )).thenReturn(Optional.of(pending));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(roomId);
            when(chatRoomRepository.findByProductIdAndBuyerAndSeller(
                    productId, buyerMember, sellerMember
            )).thenReturn(Optional.of(chatRoom));

            TradeStateSnapshot snapshotAfterWithdraw = new TradeStateSnapshot(false, false, null, null);
            when(tradeStateReader.read(product, buyerMember, sellerMember))
                    .thenReturn(snapshotAfterWithdraw);

            assertDoesNotThrow(() -> service.execute(productId, buyerId));

            verify(tradeCompleteRepository).delete(pending);

            org.mockito.ArgumentCaptor<TradeStatusChangedEvent> eventCaptor =
                    org.mockito.ArgumentCaptor.forClass(TradeStatusChangedEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

            TradeStatusChangedEvent event = eventCaptor.getValue();
            assertEquals(roomId, event.roomId());
            assertEquals(productId, event.productId());
            assertEquals(false, event.completed());
            assertEquals(false, event.reserved());
            assertEquals(null, event.requestedBySeller());
            assertEquals(null, event.requestedAt());
        }
    }
}
