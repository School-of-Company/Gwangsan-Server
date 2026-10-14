package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ReservationCancellation;
import team.startup.gwangsan.application.post.port.in.CancelReservationUseCase;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationNotificationPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.application.post.port.out.TradeStatusPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CancelReservationService implements CancelReservationUseCase {

    private final ProductPort productPort;
    private final ReservationPort reservationPort;
    private final ProductChatPort chatPort;
    private final ProductMemberPort memberPort;
    private final TradeStatusPort tradeStatusPort;
    private final ReservationNotificationPort notificationPort;

    @Override
    @Transactional
    public void execute(Long actorId, Long productId) {
        // 상품 → 예약 순서로 잠근다(거래 완료 요청과 같은 순서).
        Product product = productPort.lockActive(productId)
                .orElseThrow(NotFoundProductException::new);
        ProductReservation reservation = reservationPort.findPendingForUpdate(productId)
                .orElseThrow(ReservationParticipantOnlyException::new);
        reservation.cancel(actorId, product.getAuthorId());
        product.reopen();
        reservationPort.save(reservation);
        productPort.save(product);

        chatPort.findRoom(productId, reservation.getReserverId()).ifPresent(room -> {
            String content = memberPort.nicknameOf(actorId) + "님이 예약을 취소했어요";
            // 기존 동작: 시스템 메시지 시각은 JVM 기본 timezone, 메시지 ID 는 예약 ID 의 음수(멱등 키).
            LocalDateTime createdAt = LocalDateTime.now();
            Long messageId = chatPort.saveSystemMessage(room.roomId(), -reservation.getId(), actorId, content, createdAt);
            notificationPort.notifyCancelled(new ReservationCancellation(
                    room.roomId(), messageId, actorId, room.otherOf(actorId), content, createdAt));
            tradeStatusPort.publishChanged(room.roomId(), productId, room.buyerId(), room.sellerId());
        });
    }
}
