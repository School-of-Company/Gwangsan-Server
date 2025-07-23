package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.DeleteAdminAlertService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteAdminAlertServiceImpl implements DeleteAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = getMemberDetail(admin.getId());

        AdminAlert adminAlert = adminAlertRepository.findById(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetail alertMemberDetail = memberDetailRepository.findById(adminAlert.getRequester().getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, alertMemberDetail);

        adminAlertRepository.delete(adminAlert);
    }

    private MemberDetail getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }
}
