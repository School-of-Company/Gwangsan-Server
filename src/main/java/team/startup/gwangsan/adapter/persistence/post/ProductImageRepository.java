package team.startup.gwangsan.adapter.persistence.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>, ProductImageCustomRepository {
    void deleteByProductIdAndImageId(Long productId, Long imageId);
}