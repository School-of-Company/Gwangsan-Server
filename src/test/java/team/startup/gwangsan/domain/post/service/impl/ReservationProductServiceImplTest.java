package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationProductServiceImpl 단위 테스트")
class ReservationProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductReservationRepository productReservationRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReservationProductServiceImpl service;

    private static final Long ROOM_ID = 5L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 9, 1, 14, 0);
    private static final String LOCATION = "광산구청 1층 로비";

    @Nested
    @DisplayName("execute()는")
    class Describe_execute {

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException을 던진다")
        void throw_exception_when_product_not_found() {
            Long productId = 1L;
            Member author = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            // given
            when(productRepository.findActiveById(productId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundProductException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));

            verify(productRepository).findActiveById(productId);
        }

        @Test
        @DisplayName("호출자가 게시물 작성자가 아니면 ForbiddenProductException을 던진다")
        void throw_exception_when_not_author() {
            Long productId = 1L;

            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            Member other = mock(Member.class);
            when(other.getId()).thenReturn(2L);
            when(memberUtil.getCurrentMember()).thenReturn(other);

            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            assertThrows(ForbiddenProductException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));

            verifyNoInteractions(chatRoomRepository, productReservationRepository, applicationEventPublisher);
        }

        @Test
        @DisplayName("상품 상태가 이미 RESERVATION이면 ProductAlreadyReservationException을 던진다")
        void throw_exception_when_already_reserved() {
            Long productId = 1L;

            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            // given
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            // when & then
            assertThrows(ProductAlreadyReservationException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));
        }

        @Test
        @DisplayName("상품 상태가 ONGOING이 아니면 ProductNotOngoingException을 던진다")
        void throw_exception_when_status_is_not_ongoing() {
            Long productId = 1L;

            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            // given
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);
            when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            // when & then
            assertThrows(ProductNotOngoingException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));
        }

        @Test
        @DisplayName("채팅방을 찾을 수 없으면 NotFoundChatRoomException을 던진다")
        void throw_exception_when_chat_room_not_found() {
            Long productId = 1L;

            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));
            when(chatRoomRepository.findChatRoomByRoomId(ROOM_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundChatRoomException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));
        }

        @Test
        @DisplayName("채팅방의 상품이 요청한 상품과 다르면 NotFoundChatRoomException을 던진다")
        void throw_exception_when_chat_room_product_mismatch() {
            Long productId = 1L;

            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            Product otherProduct = mock(Product.class);
            when(otherProduct.getId()).thenReturn(999L);
            when(chatRoom.getProduct()).thenReturn(otherProduct);
            when(chatRoomRepository.findChatRoomByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

            assertThrows(NotFoundChatRoomException.class,
                    () -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));
        }

        @Test
        @DisplayName("정상 요청 시 예약 생성 후 상품 상태를 RESERVATION으로 변경한다")
        void success_when_valid_request() {
            Long productId = 1L;

            // given
            Member author = mock(Member.class);
            when(author.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(author);

            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(author);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);
            when(product.getId()).thenReturn(productId);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            Member buyer = mock(Member.class);
            when(buyer.getId()).thenReturn(2L);

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(10L);
            when(chatRoom.getProduct()).thenReturn(product);
            when(chatRoom.getBuyer()).thenReturn(buyer);
            when(chatRoomRepository.findChatRoomByRoomId(ROOM_ID)).thenReturn(Optional.of(chatRoom));

            // when
            assertDoesNotThrow(() -> service.execute(productId, ROOM_ID, SCHEDULED_AT, LOCATION));

            // then
            verify(productReservationRepository).save(any(ProductReservation.class));
            verify(product).updateStatus(ProductStatus.RESERVATION);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            TradeStatusChangedEvent event = (TradeStatusChangedEvent) eventCaptor.getValue();
            assertEquals(10L, event.roomId());
            assertNull(event.targetMemberId());
            assertEquals(productId, event.productId());
            assertFalse(event.completed());
            assertTrue(event.reserved());
        }
    }
}
