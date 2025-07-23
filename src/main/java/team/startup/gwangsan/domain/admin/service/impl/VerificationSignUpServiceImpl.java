package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.VerificationSignUpService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class VerificationSignUpServiceImpl implements VerificationSignUpService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    @Transactional
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = memberDetailRepository.findById(admin.getId())
                .orElseThrow(NotFoundMemberDetailException::new);

        AdminAlert alert = adminAlertRepository.findByIdAndType(alertId, AlertType.SIGN_UP)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetail memberDetail = memberDetailRepository.findById(alert.getSourceId())
                .orElseThrow(NotFoundMemberDetailException::new);

        validatePlaceUtil.validateSamePlace(admin, adminDetail, memberDetail);

        memberDetail.getMember().updateMemberStatus(MemberStatus.ACTIVE);

        memberDetail.plusGwangsan(5000);

        adminAlertRepository.delete(alert);
    }
}
