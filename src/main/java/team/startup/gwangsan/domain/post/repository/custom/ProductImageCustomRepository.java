package team.startup.gwangsan.domain.post.repository.custom;

import team.startup.gwangsan.domain.post.entity.ProductImage;

import java.util.Collection;
import java.util.List;

public interface ProductImageCustomRepository {
    List<ProductImage> findAllByProductIdIn(Collection<Long> productIds);

    List<ProductImage> findAllByProductId(Long productId);
}
