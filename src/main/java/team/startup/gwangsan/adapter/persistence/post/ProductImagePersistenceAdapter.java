package team.startup.gwangsan.adapter.persistence.post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductImageLinkView;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductImagePersistenceAdapter implements ProductImagePort {

    private final ProductImageRepository productImageRepository;
    private final EntityManager entityManager;

    @Override
    public List<ImageView> findImages(Long productId) {
        return productImageRepository.findAllByProductId(productId).stream()
                .map(ProductImagePersistenceAdapter::toView)
                .toList();
    }

    @Override
    public Map<Long, List<ImageView>> findImages(Collection<Long> productIds) {
        return productImageRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(pi -> pi.getProduct().getId(),
                        Collectors.mapping(ProductImagePersistenceAdapter::toView, Collectors.toList())));
    }

    @Override
    public Map<Long, List<ProductImageLinkView>> findImageLinks(Collection<Long> productIds) {
        return productImageRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(pi -> pi.getProduct().getId(),
                        Collectors.mapping(pi -> new ProductImageLinkView(pi.getId(), pi.getImage().getId(),
                                pi.getImage().getImageUrl()), Collectors.toList())));
    }

    @Override
    public void addImages(Long productId, List<Long> imageIds) {
        if (imageIds.isEmpty()) return;
        productImageRepository.saveAll(imageIds.stream().map(id -> link(productId, id)).toList());
    }

    @Override
    public Set<Long> replaceImages(Long productId, List<Long> imageIds) {
        List<ProductImage> existing = productImageRepository.findAllByProductId(productId);
        Set<Long> existingIds = existing.stream().map(pi -> pi.getImage().getId()).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(imageIds);

        Set<Long> toDelete = new HashSet<>(existingIds);
        toDelete.removeAll(requestedIds);
        // 기존 구현과 같이 HashSet 순회 순서로 추가한다(연결 행 ID 순서 = 응답 이미지 순서).
        Set<Long> toAdd = new HashSet<>(requestedIds);
        toAdd.removeAll(existingIds);

        List<ProductImage> obsolete = existing.stream().filter(pi -> toDelete.contains(pi.getImage().getId())).toList();
        if (!obsolete.isEmpty()) {
            productImageRepository.deleteAllInBatch(obsolete);
        }
        if (!toAdd.isEmpty()) {
            productImageRepository.saveAll(toAdd.stream().map(id -> link(productId, id)).toList());
        }
        return toDelete;
    }

    @Override
    public void unlink(Long productId, Long imageId) {
        productImageRepository.deleteByProductIdAndImageId(productId, imageId);
    }

    private ProductImage link(Long productId, Long imageId) {
        return ProductImage.builder()
                .product(entityManager.getReference(Product.class, productId))
                .image(entityManager.getReference(ImageJpaEntity.class, imageId))
                .build();
    }

    private static ImageView toView(ProductImage productImage) {
        return new ImageView(productImage.getImage().getId(), productImage.getImage().getImageUrl());
    }
}
