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
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.util.MemberUtil;

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

    @Nested
    @DisplayName("execute()는")
    class Describe_execute {

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException을 던진다")
        void throw_exception_when_product_not_found() {
            Long productId = 1L;

            // given
            when(productRepository.findActiveById(productId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundProductException.class,
                    () -> service.execute(productId));

            verify(productRepository).findActiveById(productId);
        }

        @Test
        @DisplayName("상품 상태가 이미 RESERVATION이면 ProductAlreadyReservationException을 던진다")
        void throw_exception_when_already_reserved() {
            Long productId = 1L;

            // given
            Product product = mock(Product.class);
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            // when & then
            assertThrows(ProductAlreadyReservationException.class,
                    () -> service.execute(productId));
        }

        @Test
        @DisplayName("상품 상태가 ONGOING이 아니면 ProductNotOngoingException을 던진다")
        void throw_exception_when_status_is_not_ongoing() {
            Long productId = 1L;

            // given
            Product product = mock(Product.class);
            when(product.getStatus()).thenReturn(ProductStatus.COMPLETED);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            // when & then
            assertThrows(ProductNotOngoingException.class,
                    () -> service.execute(productId));
        }

        @Test
        @DisplayName("정상 요청 시 예약 생성 후 상품 상태를 RESERVATION으로 변경한다")
        void success_when_valid_request() {
            Long productId = 1L;

            // given
            Member reserver = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(reserver);

            Product product = mock(Product.class);
            when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

            when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(10L);
            when(chatRoomRepository.findByProductIdAndMember(productId, reserver))
                    .thenReturn(Optional.of(chatRoom));

            // when
            assertDoesNotThrow(() -> service.execute(productId));

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
