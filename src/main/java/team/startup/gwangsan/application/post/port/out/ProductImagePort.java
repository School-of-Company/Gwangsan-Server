package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductImageLinkView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductImagePort {
    List<ImageView> findImages(Long productId);

    Map<Long, List<ImageView>> findImages(Collection<Long> productIds);

    Map<Long, List<ProductImageLinkView>> findImageLinks(Collection<Long> productIds);

    /** 주어진 순서대로 연결을 저장한다. */
    void addImages(Long productId, List<Long> imageIds);

    /** 요청 목록과 같아지도록 연결을 지우고 추가한다. 지운 이미지 ID 를 돌려준다. */
    Set<Long> replaceImages(Long productId, List<Long> imageIds);

    void unlink(Long productId, Long imageId);
}
