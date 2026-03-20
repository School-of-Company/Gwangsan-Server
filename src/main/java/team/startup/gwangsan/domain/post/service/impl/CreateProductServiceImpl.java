package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.exception.ObjectRequiredImageException;
import team.startup.gwangsan.domain.post.service.CreateProductService;
import team.startup.gwangsan.global.util.ImageValidateUtil;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductServiceImpl implements CreateProductService {

    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Type type, Mode mode, String title, String description, Integer gwangsan, List<Long> imageIds) {
        List<Long> ids = imageIds != null ? imageIds : List.of();

        if (type == Type.OBJECT && mode == Mode.GIVER && ids.isEmpty()) {
            throw new ObjectRequiredImageException();
        }

        List<Image> images = ids.isEmpty() ? List.of() : imageRepository.findByIdIn(ids);

        if (!ids.isEmpty()) {
            ImageValidateUtil.validateExistence(ids, images);
        }

        Product product = Product.builder()
                .title(title)
                .description(description)
                .gwangsan(gwangsan)
                .member(memberUtil.getCurrentMember())
                .type(type)
                .mode(mode)
                .status(ProductStatus.ONGOING)
                .build();

        productRepository.save(product);

        List<ProductImage> productImages = mapToProductImages(images, product);

        if (!productImages.isEmpty()) {
            productImageRepository.saveAll(productImages);
        }
    }

    private List<ProductImage> mapToProductImages(List<Image> images, Product product) {
        return images.stream()
                .map(image -> ProductImage.builder()
                        .image(image)
                        .product(product)
                        .build())
                .toList();
    }
}
