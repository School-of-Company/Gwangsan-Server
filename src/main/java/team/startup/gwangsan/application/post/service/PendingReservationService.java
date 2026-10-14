package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.PendingReservationView;
import team.startup.gwangsan.application.post.port.in.PendingReservationUseCase;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PendingReservationService implements PendingReservationUseCase {

    private final ReservationPort reservationPort;

    @Override
    @Transactional(readOnly = true)
    public Optional<PendingReservationView> findPending(Long productId) {
        return reservationPort.findPendingView(productId);
    }

    @Override
    public Long requireParticipant(Long productId, Long buyerId, Long sellerId) {
        ProductReservation reservation = reservationPort.findPending(productId)
                .orElseThrow(ReservationParticipantOnlyException::new);
        reservation.requireParticipant(buyerId, sellerId);
        return reservation.getId();
    }

    @Override
    public void complete(Long reservationId) {
        ProductReservation reservation = reservationPort.find(reservationId)
                .orElseThrow(ReservationParticipantOnlyException::new);
        reservation.complete();
        reservationPort.save(reservation);
    }
}
