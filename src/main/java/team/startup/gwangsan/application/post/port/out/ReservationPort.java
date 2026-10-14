package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.application.post.data.PendingReservationView;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationPort {
    /** 상품의 PENDING 예약을 찾아 잠근다. 호출자는 상품을 먼저 잠가 상품→예약 잠금 순서를 지킨다. */
    Optional<ProductReservation> findPendingForUpdate(Long productId);

    /** 잠금 없이 상품의 PENDING 예약을 찾는다. */
    Optional<ProductReservation> findPending(Long productId);

    Optional<PendingReservationView> findPendingView(Long productId);

    Optional<ProductReservation> find(Long reservationId);

    /** bulk UPDATE + 영속성 컨텍스트 clear. */
    void reassignReserver(Long memberId, Long dummyMemberId);

    Optional<Long> findProductIdOf(Long reservationId);

    Optional<ProductReservation> lock(Long reservationId);

    List<Long> findDueReminderIds(LocalDateTime now, LocalDateTime reminderBefore);

    ProductReservation save(ProductReservation reservation);
}
