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
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class VerificationSignUpServiceImpl implements VerificationSignUpService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final WithdrawalRecordRepository withdrawalRecordRepository;

    private static final int SIGNUP_GWANGSAN_REWARD = 10000;

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
        Member member = memberDetail.getMember();

        validatePlaceUtil.validateSamePlace(admin, adminDetail, memberDetail);

        member.updateMemberStatus(MemberStatus.ACTIVE);

        WithdrawalRecord withdrawalRecord = withdrawalRecordRepository.findByPhoneNumber(member.getPhoneNumber())
                .orElse(null);

        applyGwangsanPolicy(memberDetail, withdrawalRecord);

        adminAlertRepository.delete(alert);
    }

    private void applyGwangsanPolicy(MemberDetail memberDetail, WithdrawalRecord record) {
        if (record != null) {
            memberDetail.plusGwangsan(record.getGwangsan());
            withdrawalRecordRepository.delete(record);
        } else {
            memberDetail.plusGwangsan(SIGNUP_GWANGSAN_REWARD);
        }
    }
}
