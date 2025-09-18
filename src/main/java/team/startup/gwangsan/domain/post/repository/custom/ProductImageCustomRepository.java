package team.startup.gwangsan.domain.post.repository.custom;

import team.startup.gwangsan.domain.post.entity.ProductImage;

import java.util.List;

public interface ProductImageCustomRepository {
    List<ProductImage> findProductImageByProductIdIn(List<Long> productIds);

    List<ProductImage> findAllByProductId(Long productId);
}
