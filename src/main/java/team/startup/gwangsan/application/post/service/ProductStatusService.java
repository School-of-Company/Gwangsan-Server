package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductStatusService implements ProductStatusUseCase {

    private final ProductPort productPort;

    @Override
    public ProductSnapshot lockActive(Long productId) {
        return productPort.lockActive(productId).map(ProductSnapshot::from).orElseThrow(NotFoundProductException::new);
    }

    @Override
    public ProductSnapshot lockIncludingDeleted(Long productId) {
        return productPort.lockIncludingDeleted(productId).map(ProductSnapshot::from)
                .orElseThrow(NotFoundProductException::new);
    }

    @Override
    public void complete(Long productId) {
        change(productPort.find(productId).orElseThrow(NotFoundProductException::new), Product::complete);
    }

    @Override
    public void reopen(Long productId) {
        change(productPort.find(productId).orElseThrow(NotFoundProductException::new), Product::reopen);
    }

    @Override
    public void deleteByAdmin(Long productId) {
        change(productPort.findActive(productId).orElseThrow(NotFoundProductException::new), Product::deleteByAdmin);
    }

    private void change(Product product, Consumer<Product> transition) {
        transition.accept(product);
        productPort.save(product);
    }
}
