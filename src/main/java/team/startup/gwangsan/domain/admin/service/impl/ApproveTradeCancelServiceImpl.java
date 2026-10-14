package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.ApproveTradeCancelService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.global.util.MemberUtil;

/**
 * 관리자 철회 승인 진입. 철회 반영(잠금 순서·환불·상태 전이)은 trade 의 ApproveTradeCancelUseCase 가 소유하고,
 * 여기서는 관리자 인가만 남긴다. legacy bridge: PR11 에서 admin application 으로 전환한다.
 */
@Service
@RequiredArgsConstructor
public class ApproveTradeCancelServiceImpl implements ApproveTradeCancelService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final ApproveTradeCancelUseCase approveTradeCancelUseCase;

    /** 잠금 순서와 READ_COMMITTED 이유는 trade ApproveTradeCancelService 주석 참고. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(Long alertId) {
        MemberJpaEntity admin = memberUtil.getCurrentMember();
        AdminAlert alert = adminAlertRepository.findByIdWithMember(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetailJpaEntity adminDetail = getMemberDetail(admin.getId());

        approveTradeCancelUseCase.execute(new ApproveTradeCancelCommand(alert.getSourceId(), (buyerId, sellerId) -> {
            MemberDetailJpaEntity buyerDetail = getMemberDetail(buyerId);
            MemberDetailJpaEntity sellerDetail = getMemberDetail(sellerId);
            validatePlaceUtil.validateSamePlace(admin, adminDetail, buyerDetail);
            validatePlaceUtil.validateSamePlace(admin, adminDetail, sellerDetail);
        }));
    }

    private MemberDetailJpaEntity getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }
}
