package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductPort {
    Optional<Product> findActive(Long productId);

    /** 삭제되지 않은 게시글을 PESSIMISTIC_WRITE 로 잠근다. 거래·예약·삭제의 상태 변경을 직렬화한다. */
    Optional<Product> lockActive(Long productId);

    /** 삭제된 게시글 포함. */
    Optional<Product> find(Long productId);

    /** 삭제된 게시글 포함 PESSIMISTIC_WRITE 잠금. */
    Optional<Product> lockIncludingDeleted(Long productId);

    /** 삭제된 게시글 포함. */
    List<Product> findAll(Collection<Long> productIds);

    /** 삭제 제외, id 내림차순 cursor 페이지(관리자 목록). */
    List<Product> findActivePage(Type type, Mode mode, Long lastId, int size);

    /** bulk UPDATE + 영속성 컨텍스트 clear. */
    void reassignAuthor(Long memberId, Long dummyMemberId);

    /** 상태를 저장 모델에 명시적으로 반영한다. 새 게시글이면 id 를 채워 돌려준다. */
    Product save(Product product);

    List<Product> findOngoingInPlace(Type type, Mode mode, Integer placeId);

    List<Product> findByAuthor(Long authorId, Type type, Mode mode, Collection<ProductStatus> statuses);

    /** id 내림차순 cursor 페이지. completed: null=전체, true=완료, false=진행/예약. */
    List<Product> findAuthorPage(Long authorId, Type type, Mode mode, Long lastId, int size, Boolean completed);
}
