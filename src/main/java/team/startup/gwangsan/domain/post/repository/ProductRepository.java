package team.startup.gwangsan.domain.post.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.repository.custom.ProductCustomRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductCustomRepository {
    Optional<Product> findByIdAndStatusNot(Long id, ProductStatus status);

    /**
     * 삭제되지 않은 게시글만 조회한다. 단건 조회는 이 메서드를 사용한다.
     */
    default Optional<Product> findActiveById(Long id) {
        return findByIdAndStatusNot(id, ProductStatus.DELETED);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.status <> team.startup.gwangsan.domain.post.entity.constant.ProductStatus.DELETED")
    Optional<Product> findByIdWithLock(Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.member = :dummy WHERE p.member = :target")
    void reassignMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
