package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductNotReservationException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.DeleteReservationProductService;

@Service
@RequiredArgsConstructor
public class DeleteReservationProductServiceImpl implements DeleteReservationProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() != ProductStatus.RESERVATION) {
            throw new ProductNotReservationException();
        }

        product.updateStatus(ProductStatus.ONGOING);
    }
}
