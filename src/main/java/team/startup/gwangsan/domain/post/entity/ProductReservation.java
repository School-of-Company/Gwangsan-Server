package team.startup.gwangsan.domain.post.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "tbl_product_reservation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "status"})
)
@EntityListeners(AuditingEntityListener.class)
public class ProductReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_reservation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserver_id")
    private Member reserver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public ProductReservation(Product product, Member reserver, ReservationStatus status, LocalDateTime scheduledAt,
                               String placeName, String address, Double latitude, Double longitude) {
        this.product = product;
        this.reserver = reserver;
        this.status = status == null ? ReservationStatus.PENDING : status;
        this.scheduledAt = scheduledAt;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }
}
