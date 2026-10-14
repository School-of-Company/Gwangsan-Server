package team.startup.gwangsan.adapter.persistence.post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.PendingReservationView;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements ReservationPort {

    private final ProductReservationRepository reservationRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<ProductReservation> findPendingForUpdate(Long productId) {
        return reservationRepository.findByProductAndStatus(product(productId), ReservationStatus.PENDING)
                .flatMap(found -> reservationRepository.findByIdForUpdate(found.getId()))
                .map(ReservationPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ProductReservation> findPending(Long productId) {
        return reservationRepository.findByProductAndStatus(product(productId), ReservationStatus.PENDING)
                .map(ReservationPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<PendingReservationView> findPendingView(Long productId) {
        return reservationRepository.findByProductAndStatus(product(productId), ReservationStatus.PENDING)
                .map(r -> new PendingReservationView(r.getId(), productId, r.getReserver().getId(), r.getScheduledAt(),
                        r.getCreatedAt(), r.getPlaceName(), r.getAddress(), r.getLatitude(), r.getLongitude()));
    }

    @Override
    public Optional<ProductReservation> find(Long reservationId) {
        return reservationRepository.findById(reservationId).map(ReservationPersistenceAdapter::toDomain);
    }

    @Override
    public void reassignReserver(Long memberId, Long dummyMemberId) {
        reservationRepository.reassignReserver(entityManager.getReference(MemberJpaEntity.class, memberId),
                entityManager.getReference(MemberJpaEntity.class, dummyMemberId));
    }

    @Override
    public Optional<Long> findProductIdOf(Long reservationId) {
        return reservationRepository.findProductIdById(reservationId);
    }

    @Override
    public Optional<ProductReservation> lock(Long reservationId) {
        return reservationRepository.findByIdForUpdate(reservationId).map(ReservationPersistenceAdapter::toDomain);
    }

    @Override
    public List<Long> findDueReminderIds(LocalDateTime now, LocalDateTime reminderBefore) {
        return reservationRepository.findDueReminderIds(now, reminderBefore);
    }

    @Override
    public ProductReservation save(ProductReservation reservation) {
        if (reservation.getId() == null) {
            team.startup.gwangsan.adapter.persistence.post.ProductReservation entity =
                    team.startup.gwangsan.adapter.persistence.post.ProductReservation.builder()
                            .product(product(reservation.getProductId()))
                            .reserver(entityManager.getReference(MemberJpaEntity.class, reservation.getReserverId()))
                            .status(reservation.getStatus())
                            .scheduledAt(reservation.getScheduledAt())
                            .placeName(reservation.getPlaceName())
                            .address(reservation.getAddress())
                            .latitude(reservation.getLatitude())
                            .longitude(reservation.getLongitude())
                            .build();
            if (reservation.getReminder30SentAt() != null) {
                entity.markReminder30Sent(reservation.getReminder30SentAt());
            }
            return toDomain(reservationRepository.save(entity));
        }
        team.startup.gwangsan.adapter.persistence.post.ProductReservation entity =
                reservationRepository.findById(reservation.getId()).orElseThrow(IllegalStateException::new);
        if (reservation.getStatus() == ReservationStatus.CANCELLED && entity.getStatus() != ReservationStatus.CANCELLED) {
            // Entity 가 cancelledAt 을 기존과 같은 시각 기준(JVM 기본 timezone)으로 기록한다.
            entity.cancel();
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED && entity.getStatus() != ReservationStatus.COMPLETED) {
            entity.complete();
        }
        if (entity.getReminder30SentAt() == null && reservation.getReminder30SentAt() != null) {
            entity.markReminder30Sent(reservation.getReminder30SentAt());
        }
        if (entity.getReminderAtSentAt() == null && reservation.getReminderAtSentAt() != null) {
            entity.markReminderAtSent(reservation.getReminderAtSentAt());
        }
        return reservation;
    }

    private team.startup.gwangsan.adapter.persistence.post.Product product(Long productId) {
        return entityManager.getReference(team.startup.gwangsan.adapter.persistence.post.Product.class, productId);
    }

    private static ProductReservation toDomain(team.startup.gwangsan.adapter.persistence.post.ProductReservation entity) {
        return new ProductReservation(entity.getId(), entity.getProduct().getId(), entity.getReserver().getId(),
                entity.getStatus(), entity.getScheduledAt(), entity.getPlaceName(), entity.getAddress(),
                entity.getLatitude(), entity.getLongitude(), entity.getReminder30SentAt(), entity.getReminderAtSentAt());
    }
}
