package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.ReservationProductService;

@Service
@RequiredArgsConstructor
public class ReservationProductServiceImpl implements ReservationProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() == ProductStatus.RESERVATION) {
            throw new ProductAlreadyReservationException();
        }

        if (product.getStatus() != ProductStatus.ONGOING) {
            throw new ProductNotOngoingException();
        }

        product.updateStatus(ProductStatus.RESERVATION);
    }
}
