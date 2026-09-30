package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface ProductReservationRepository extends JpaRepository<ProductReservation, Long> {
    @EntityGraph(attributePaths = "reserver")
    Optional<ProductReservation> findByProductAndStatus(Product product, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ProductReservation r where r.id = :id")
    Optional<ProductReservation> findByIdForUpdate(@Param("id") Long id);

    @Query("select r.product.id from ProductReservation r where r.id = :id")
    Optional<Long> findProductIdById(@Param("id") Long id);

    @Query("""
            select r.id from ProductReservation r
            where r.status = team.startup.gwangsan.domain.post.entity.constant.ReservationStatus.PENDING
              and r.scheduledAt is not null
              and ((r.reminder30SentAt is null and r.scheduledAt <= :before)
                or (r.reminderAtSentAt is null and r.scheduledAt <= :now))
            """)
    List<Long> findDueReminderIds(@Param("now") LocalDateTime now,
                                  @Param("before") LocalDateTime before);
}
