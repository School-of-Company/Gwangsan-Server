package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.UpdateProductCommand;
import team.startup.gwangsan.application.post.port.in.UpdateProductUseCase;
import team.startup.gwangsan.application.post.port.out.ProductImageAssetPort;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateProductService implements UpdateProductUseCase {

    private final ProductPort productPort;
    private final ProductImagePort productImagePort;
    private final ProductImageAssetPort imageAssetPort;

    @Override
    @Transactional
    public void execute(UpdateProductCommand command) {
        Product product = productPort.findActive(command.productId())
                .orElseThrow(NotFoundProductException::new);
        List<Long> imageIds = command.imageIds() != null ? command.imageIds() : List.of();
        product.update(command.actorId(), command.type(), command.mode(), command.title(),
                command.description(), command.gwangsan(), imageIds);
        imageAssetPort.requireExisting(imageIds);

        productPort.save(product);
        Set<Long> removedImageIds = productImagePort.replaceImages(product.getId(), imageIds);
        imageAssetPort.requestUnusedCleanup(product.getId(), removedImageIds);
    }
}
