package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.CompleteTradeService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompleteTradeServiceImpl implements CompleteTradeService {

    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final PlaceRepository placeRepository;
    private final ProductRepository productRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public void execute(Long productId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = findMemberDetail(admin.getId());

        List<Place> places = getAdminPlaces(admin, adminDetail);

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);

        MemberDetail productOwnerDetail = findMemberDetail(product.getMember().getId());
        validatePlaceAuthority(places, productOwnerDetail.getPlace());

        List<Long> memberIds = new ArrayList<>();
        memberIds.add(admin.getId());
        memberIds.add(productOwnerDetail.getId());

        AdminAlert adminAlert = adminAlertRepository.findBySourceId(productId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetail counterpartDetail = findMemberDetail(adminAlert.getOtherMember().getId());

        settleGwangsan(product, productOwnerDetail, counterpartDetail);

        product.updateStatus(ProductStatus.COMPLETED);
        adminAlertRepository.delete(adminAlert);

        List<String> deviceTokens = new ArrayList<>();
        for (Long memberId : memberIds) {
            deviceTokenRepository.findByUserId(memberId)
                    .map(DeviceToken::getDeviceToken)
                    .ifPresent(deviceTokens::add);
        }

        applicationEventPublisher.publishEvent(new SendNotificationEvent(
                deviceTokens,
                NotificationType.TRADE_COMPLETE
                ));
    }

    private MemberDetail findMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }

    private List<Place> getAdminPlaces(Member admin, MemberDetail detail) {
        return admin.getRole() == MemberRole.ROLE_HEAD_ADMIN
                ? placeRepository.findByHead(detail.getPlace().getHead())
                : List.of(detail.getPlace());
    }

    private void validatePlaceAuthority(List<Place> places, Place targetPlace) {
        boolean hasAuthority = places.stream()
                .anyMatch(p -> p.getId().equals(targetPlace.getId()));
        if (!hasAuthority) throw new NotFoundMemberDetailException();
    }

    private void settleGwangsan(Product product, MemberDetail owner, MemberDetail other) {
        int gwangsan = product.getGwangsan();
        if (product.getMode() == Mode.GIVER) {
            owner.plusGwangsan(gwangsan);
            other.minusGwangsan(gwangsan);
        } else {
            owner.minusGwangsan(gwangsan);
            other.plusGwangsan(gwangsan);
        }
    }
}
