package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.UpdateProductService;
import team.startup.gwangsan.global.event.DeleteNotUsedImageEvent;
import team.startup.gwangsan.global.event.constant.ImageType;
import team.startup.gwangsan.global.util.ImageValidateUtil;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateProductServiceImpl implements UpdateProductService {

    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long productId, Type type, Mode mode, String title, String description, Integer gwangsan, List<Long> imageIds) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        validateProductMember(member, product);

        product.update(type, mode, title, description, gwangsan);

        List<Image> images = imageRepository.findByIdIn(imageIds);
        ImageValidateUtil.validateExistence(imageIds, images);

        List<ProductImage> existingImages = productImageRepository.findByProductId(productId);

        Set<Long> existingImageIds = extractImageIds(existingImages);
        Set<Long> requestImageIds = new HashSet<>(imageIds);

        Set<Long> toDeleteImageIds = findToDeleteIds(existingImageIds, requestImageIds);
        Set<Long> toAddImageIds = findToAddIds(existingImageIds, requestImageIds);

        List<ProductImage> toSave = buildProductImagesToSave(toAddImageIds, images, product);

        productImageRepository.saveAll(toSave);

        applicationEventPublisher.publishEvent(new DeleteNotUsedImageEvent(productId, toDeleteImageIds, ImageType.PRODUCT));
    }

    private Set<Long> extractImageIds(List<ProductImage> productImages) {
        return productImages.stream()
                .map(pi -> pi.getImage().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> findToDeleteIds(Set<Long> existingIds, Set<Long> requestedIds) {
        Set<Long> toDelete = new HashSet<>(existingIds);
        toDelete.removeAll(requestedIds);
        return toDelete;
    }

    private Set<Long> findToAddIds(Set<Long> existingIds, Set<Long> requestedIds) {
        Set<Long> toAdd = new HashSet<>(requestedIds);
        toAdd.removeAll(existingIds);
        return toAdd;
    }

    private List<ProductImage> buildProductImagesToSave(Set<Long> toAddImageIds, List<Image> images, Product product) {
        return toAddImageIds.stream()
                .map(id -> {
                    Image image = images.stream()
                            .filter(img -> img.getId().equals(id))
                            .findFirst()
                            .orElseThrow();
                    return ProductImage.builder()
                            .product(product)
                            .image(image)
                            .build();
                })
                .toList();
    }

    private void validateProductMember(Member member, Product product) {
        if (!product.getMember().getId().equals(member.getId())) {
            throw new ForbiddenProductException();
        }
    }
}
