package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductImageLinkView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPublicQueryService implements ProductSnapshotQuery, ProductImageQuery {

    private final ProductPort productPort;
    private final ProductImagePort productImagePort;

    @Override
    public ProductSnapshot getActive(Long productId) {
        return productPort.findActive(productId).map(ProductSnapshot::from).orElseThrow(NotFoundProductException::new);
    }

    @Override
    public ProductSnapshot get(Long productId) {
        return productPort.find(productId).map(ProductSnapshot::from).orElseThrow(NotFoundProductException::new);
    }

    @Override
    public Map<Long, ProductSnapshot> findAll(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productPort.findAll(productIds).stream()
                .map(ProductSnapshot::from)
                .collect(Collectors.toMap(ProductSnapshot::id, Function.identity()));
    }

    @Override
    public List<ProductSnapshot> findActivePage(Type type, Mode mode, Long lastId, int size) {
        return productPort.findActivePage(type, mode, lastId, size).stream().map(ProductSnapshot::from).toList();
    }

    @Override
    public List<ImageView> findImages(Long productId) {
        return productImagePort.findImages(productId);
    }

    @Override
    public Map<Long, List<ImageView>> findImages(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImagePort.findImages(productIds);
    }

    @Override
    public Map<Long, List<ProductImageLinkView>> findImageLinks(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImagePort.findImageLinks(productIds);
    }
}
