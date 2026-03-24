package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.post.service.ReservationProductService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class ReservationProductServiceImpl implements ReservationProductService {

    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long productId) {
        Member reserver = memberUtil.getCurrentMember();

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        if (product.getStatus() == ProductStatus.RESERVATION) {
            throw new ProductAlreadyReservationException();
        }

        if (product.getStatus() != ProductStatus.ONGOING) {
            throw new ProductNotOngoingException();
        }

        productReservationRepository.save(
                ProductReservation.builder()
                        .product(product)
                        .reserver(reserver)
                        .status(ReservationStatus.PENDING)
                        .build()
        );

        product.updateStatus(ProductStatus.RESERVATION);
    }
}
