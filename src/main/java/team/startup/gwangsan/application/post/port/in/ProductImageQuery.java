package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductImageLinkView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 게시글 이미지 공개 조회. 순서는 연결 행 저장 순서(기존 조회와 같음). */
public interface ProductImageQuery {

    List<ImageView> findImages(Long productId);

    /** 한 번의 쿼리로 묶어 조회한다. 이미지가 없는 게시글은 key 가 없다. 빈 입력이면 쿼리 없이 빈 Map. */
    Map<Long, List<ImageView>> findImages(Collection<Long> productIds);

    /** findImages(Collection) 와 같은 1 쿼리. 연결 행 ID 가 필요한 소비자(알림 응답)용. 빈 입력이면 쿼리 없이 빈 Map. */
    Map<Long, List<ProductImageLinkView>> findImageLinks(Collection<Long> productIds);
}
