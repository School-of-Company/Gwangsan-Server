package team.startup.gwangsan.adapter.integration.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.review.data.ReviewImage;
import team.startup.gwangsan.application.review.port.out.ReviewProductPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** review → post 공개 Port 연결. */
@Component
@RequiredArgsConstructor
public class ReviewPostAdapter implements ReviewProductPort {

    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;

    @Override
    public boolean isCompleted(long productId) {
        return productSnapshotQuery.getActive(productId).isCompleted();
    }

    @Override
    public Map<Long, List<ReviewImage>> findImages(List<Long> productIds) {
        Map<Long, List<ReviewImage>> images = new LinkedHashMap<>();
        productImageQuery.findImages(productIds).forEach((productId, views) -> images.put(productId, toImages(views)));
        return images;
    }

    @Override
    public List<ReviewImage> findImages(long productId) {
        return toImages(productImageQuery.findImages(productId));
    }

    private static List<ReviewImage> toImages(List<ImageView> views) {
        return views.stream().map(v -> new ReviewImage(v.imageId(), v.imageUrl())).toList();
    }
}
