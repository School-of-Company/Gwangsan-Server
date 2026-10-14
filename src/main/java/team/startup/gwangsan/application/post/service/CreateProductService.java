package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.CreateProductCommand;
import team.startup.gwangsan.application.post.port.in.CreateProductUseCase;
import team.startup.gwangsan.application.post.port.out.ProductImageAssetPort;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductPort productPort;
    private final ProductImagePort productImagePort;
    private final ProductImageAssetPort imageAssetPort;

    @Override
    @Transactional
    public void execute(CreateProductCommand command) {
        List<Long> imageIds = command.imageIds() != null ? command.imageIds() : List.of();
        Product product = Product.create(command.actorId(), command.type(), command.mode(), command.title(),
                command.description(), command.gwangsan(), imageIds);
        List<Long> existingImageIds = imageAssetPort.requireExisting(imageIds);

        Product saved = productPort.save(product);
        productImagePort.addImages(saved.getId(), existingImageIds);
    }
}
