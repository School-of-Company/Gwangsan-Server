package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAlertTypeException;
import team.startup.gwangsan.domain.admin.exception.NotFoundPendingMemberException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.CreateAdminAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;

@Service
@RequiredArgsConstructor
public class CreateAdminAlertServiceImpl implements CreateAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    private static final String REPORT_FRAUD_ALERT_TITLE = "부적절한 거래 신고";
    private static final String REPORT_BAD_LANGUAGE_ALERT_TITLE = "부적절한 언어 사용";
    private static final String REPORT_MEMBER_ALERT_TITLE = "부적절한 사용자 신고";
    private static final String REPORT_ETC_ALERT_TITLE = "기타 신고";
    private static final String NEW_SIGNUP_MEMBER_TITLE = "새로운 회원가입 요청";

    @Override
    @Transactional
    public void execute(AlertType type, Long sourceId, Member member) {
        switch (type) {
            case REPORT -> {
                Report report = reportRepository.findById(sourceId)
                        .orElseThrow(NotFoundReportException::new);

                String alertTile = resolveReportAlertTitle(report);

                AdminAlert alert = AdminAlert.builder()
                        .type(type)
                        .title(alertTile)
                        .sourceId(sourceId)
                        .member(member)
                        .build();
                saveAdminAlert(alert);
            }
            case SIGN_UP -> {
                Member signUpMember = memberRepository.findByStatusAndId(MemberStatus.PENDING, sourceId)
                        .orElseThrow(NotFoundPendingMemberException::new);

                AdminAlert alert = AdminAlert.builder()
                        .type(type)
                        .title(NEW_SIGNUP_MEMBER_TITLE)
                        .sourceId(sourceId)
                        .member(signUpMember)
                        .build();

                saveAdminAlert(alert);
            }
            default -> {
                throw new NotFoundAlertTypeException();
            }
        }
    }

    private String resolveReportAlertTitle(Report report) {
        return switch (report.getReportType()) {
            case FRAUD -> REPORT_FRAUD_ALERT_TITLE;
            case BAD_LANGUAGE -> REPORT_BAD_LANGUAGE_ALERT_TITLE;
            case MEMBER -> REPORT_MEMBER_ALERT_TITLE;
            case ETC -> REPORT_ETC_ALERT_TITLE;
        };
    }

    private void saveAdminAlert(AdminAlert alert) {
        adminAlertRepository.save(alert);
    }
}
