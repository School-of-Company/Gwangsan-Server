package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.DeleteAdminAlertService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteAdminAlertServiceImpl implements DeleteAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    public void execute(Long alertId) {
        MemberJpaEntity admin = memberUtil.getCurrentMember();
        MemberDetailJpaEntity adminDetail = getMemberDetail(admin.getId());

        AdminAlert adminAlert = adminAlertRepository.findById(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetailJpaEntity alertMemberDetail = memberDetailRepository.findById(adminAlert.getRequester().getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, alertMemberDetail);

        adminAlertRepository.delete(adminAlert);
    }

    private MemberDetailJpaEntity getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }
}
