package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.post.service.DeleteReservationProductService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteReservationProductServiceImpl implements DeleteReservationProductService {

    private final ProductRepository productRepository;
    private final MemberUtil memberUtil;
    private final ProductReservationRepository productReservationRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Member member = memberUtil.getCurrentMember();

        ProductReservation productReservation = productReservationRepository
                .findByProduct_MemberOrReserverAndStatus(member, member, ReservationStatus.PENDING)
                .orElseThrow(ReservationParticipantOnlyException::new);

        Product product = productReservation.getProduct();

        productReservation.cancel();

        product.updateStatus(ProductStatus.ONGOING);
    }
}
