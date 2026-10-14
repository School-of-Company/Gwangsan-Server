package team.startup.gwangsan.adapter.persistence.post;


import java.util.Collection;
import java.util.List;

public interface ProductImageCustomRepository {
    List<ProductImage> findAllByProductIdIn(Collection<Long> productIds);

    List<ProductImage> findAllByProductId(Long productId);
}
