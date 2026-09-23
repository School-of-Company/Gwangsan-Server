package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.DeleteAdminProductService;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class DeleteAdminProductServiceImpl implements DeleteAdminProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Product product = productRepository.findActiveById(productId)
                .orElseThrow(NotFoundProductException::new);
        product.updateStatus(ProductStatus.DELETED);
    }
}
