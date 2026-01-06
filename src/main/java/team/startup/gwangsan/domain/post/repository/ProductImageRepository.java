package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.custom.ProductImageCustomRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>, ProductImageCustomRepository {
    void deleteByProductIdAndImageId(Long productId, Long imageId);
    List<ProductImage> findAllByProduct_IdIn(List<Long> productIds);
}