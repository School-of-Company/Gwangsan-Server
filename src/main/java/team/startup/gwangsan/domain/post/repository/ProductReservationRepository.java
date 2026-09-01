package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;

import java.util.Optional;

public interface ProductReservationRepository extends JpaRepository<ProductReservation, Long> {
    @EntityGraph(attributePaths = "reserver")
    Optional<ProductReservation> findByProductAndStatus(Product product, ReservationStatus status);
}
