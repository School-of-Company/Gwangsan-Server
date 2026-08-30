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
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteReservationProductServiceImpl 단위 테스트")
class DeleteReservationProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ProductReservationRepository productReservationRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock

    private TradeStateReader tradeStateReader;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private DeleteReservationProductServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("예약자 또는 상품 등록자가 호출하면 예약을 취소하고 상품 상태를 ONGOING 으로 변경한다")
        void it_cancels_reservation_and_update_product_status_when_called_by_participant() {
            // given
            Long productId = 1L;

            Member currentMember = mock(Member.class);
            Member reserver = mock(Member.class);
            ProductReservation reservation = mock(ProductReservation.class);
            Product product = mock(Product.class);

            when(memberUtil.getCurrentMember()).thenReturn(currentMember);
            when(productReservationRepository.findByProduct_MemberOrReserverAndStatus(
                    currentMember,
                    currentMember,
                    ReservationStatus.PENDING
            )).thenReturn(Optional.of(reservation));
            when(reservation.getProduct()).thenReturn(product);
            when(reservation.getReserver()).thenReturn(reserver);
            when(product.getId()).thenReturn(productId);

            ChatRoom chatRoom = mock(ChatRoom.class);
            when(chatRoom.getId()).thenReturn(10L);
            when(chatRoomRepository.findByProductIdAndMember(productId, reserver))
                    .thenReturn(Optional.of(chatRoom));

            // when & then
            when(tradeStateReader.read(any(), any(), any()))
                    .thenReturn(new TradeStateSnapshot(false, false, null, null));
            assertDoesNotThrow(() -> service.execute(productId));

            verify(memberUtil).getCurrentMember();
            verify(productReservationRepository).findByProduct_MemberOrReserverAndStatus(
                    currentMember,
                    currentMember,
                    ReservationStatus.PENDING
            );
            verify(reservation).cancel();
            verify(product).updateStatus(ProductStatus.ONGOING);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            TradeStatusChangedEvent event = (TradeStatusChangedEvent) eventCaptor.getValue();
            assertEquals(10L, event.roomId());
            assertEquals(productId, event.productId());
            assertFalse(event.completed());
            assertFalse(event.reserved());
            assertNull(event.requestedBySeller());
            assertNull(event.requestedAt());

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("예약자/상품 등록자가 아닌 사용자가 호출하면 ReservationParticipantOnlyException 을 던진다")
        void it_throws_ReservationParticipantOnlyException_when_not_participant() {
            // given
            Long productId = 1L;

            Member currentMember = mock(Member.class);

            when(memberUtil.getCurrentMember()).thenReturn(currentMember);
            when(productReservationRepository.findByProduct_MemberOrReserverAndStatus(
                    currentMember,
                    currentMember,
                    ReservationStatus.PENDING
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(ReservationParticipantOnlyException.class,
                    () -> service.execute(productId));

            verify(memberUtil).getCurrentMember();
            verify(productReservationRepository).findByProduct_MemberOrReserverAndStatus(
                    currentMember,
                    currentMember,
                    ReservationStatus.PENDING
            );

            verifyNoMoreInteractions(productReservationRepository);
            verifyNoInteractions(productRepository);
        }
    }
}
