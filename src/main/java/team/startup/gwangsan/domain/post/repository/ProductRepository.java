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

    /**
     * 거래를 새로 진행하기 위한 잠금. 삭제된 게시글은 제외한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.status <> team.startup.gwangsan.domain.post.entity.constant.ProductStatus.DELETED")
    Optional<Product> findByIdWithLock(Long id);

    /**
     * 이미 성립한 거래를 되돌리기 위한 잠금. 삭제된 게시글도 포함한다.
     *
     * <p>게시글 삭제는 거래 상태를 검사하지 않으므로(DeleteProductByIdServiceImpl), 철회 대기 중에
     * 판매자가 글을 지울 수 있다. 여기서 DELETED 를 걸러내면 광산 환불이 영구히 막히고 관리자
     * 알림도 처리 불가 상태로 남는다. 잠그는 행은 {@link #findByIdWithLock} 과 같으므로
     * 거래 완료 요청 경로와의 직렬화는 그대로 유지된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.member = :dummy WHERE p.member = :target")
    void reassignMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
