package team.startup.gwangsan.adapter.persistence.post;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.post.model.ReservationStatus;

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
            where r.status = team.startup.gwangsan.domain.post.model.ReservationStatus.PENDING
              and r.scheduledAt is not null
              and ((r.reminder30SentAt is null and r.scheduledAt <= :before)
                or (r.reminderAtSentAt is null and r.scheduledAt <= :now))
            """)
    List<Long> findDueReminderIds(@Param("now") LocalDateTime now,
                                  @Param("before") LocalDateTime before);

    /** 회원 탈퇴: 예약자를 dummy 회원으로 재지정한다. fix/member-withdrawal-reference-cleanup 와 같은 쿼리. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductReservation r SET r.reserver = :dummy WHERE r.reserver = :target")
    void reassignReserver(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
