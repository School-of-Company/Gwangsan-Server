package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 게시글 공개 조회(trade·review·chat·report·alert·admin 소비). 잠금 없음.
 * 없는 게시글은 NotFoundProductException(404) 을 던진다.
 */
public interface ProductSnapshotQuery {

    /** 삭제되지 않은 게시글. */
    ProductSnapshot getActive(Long productId);

    /** 삭제된 게시글 포함. */
    ProductSnapshot get(Long productId);

    /** 삭제된 게시글 포함. 없는 ID 는 결과에서 빠진다. 빈 입력이면 쿼리 없이 빈 Map. */
    Map<Long, ProductSnapshot> findAll(Collection<Long> productIds);

    /** 관리자 목록: 삭제 제외, id 내림차순 cursor(lastId 미만), type·mode 는 null 이면 조건 없음. */
    List<ProductSnapshot> findActivePage(Type type, Mode mode, Long lastId, int size);
}
