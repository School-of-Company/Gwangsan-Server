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
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;

@Service
@RequiredArgsConstructor
public class CreateAdminAlertServiceImpl implements CreateAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final TradeCancelRepository tradeCancelRepository;

    private static final String REPORT_SEXUAL_ALERT_TITLE = "음란/성적 콘텐츠 신고";
    private static final String REPORT_ABUSE_HATE_HARASSMENT_ALERT_TITLE = "욕설/혐오/괴롭힘 신고";
    private static final String REPORT_SPAM_AD_ALERT_TITLE = "스팸/광고 신고";
    private static final String REPORT_IMPERSONATION_ALERT_TITLE = "사칭 신고";
    private static final String REPORT_SELF_HARM_DANGER_ALERT_TITLE = "자해/위험 신고";
    private static final String REPORT_ETC_ALERT_TITLE = "기타 신고";
    private static final String NEW_SIGNUP_MEMBER_TITLE = "새로운 회원가입 요청";
    private static final String TRADE_CANCEL_TITLE = "거래 철회 요청";

    @Override
    @Transactional
    public void execute(AlertType type, Long sourceId, Long memberId) {
        switch (type) {
            case REPORT -> {
                Report report = reportRepository.findById(sourceId)
                        .orElseThrow(NotFoundReportException::new);

                String alertTile = resolveReportAlertTitle(report);

                Member member = memberRepository.findById(memberId)
                        .orElseThrow(NotFoundMemberException::new);

                AdminAlert alert = AdminAlert.builder()
                        .type(type)
                        .title(alertTile)
                        .sourceId(sourceId)
                        .requester(member)
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
                        .requester(signUpMember)
                        .build();

                saveAdminAlert(alert);
            }

            case TRADE_CANCEL -> {
                TradeCancel tradeCancel = tradeCancelRepository.findByIdWithMember(sourceId)
                        .orElseThrow(NotFoundTradeCancelException::new);

                AdminAlert alert = AdminAlert.builder()
                        .type(type)
                        .title(TRADE_CANCEL_TITLE)
                        .sourceId(sourceId)
                        .requester(tradeCancel.getMember())
                        .build();

                saveAdminAlert(alert);
            }

            default -> throw new NotFoundAlertTypeException();
        }
    }

    private String resolveReportAlertTitle(Report report) {
        return switch (report.getReportType()) {
            case SEXUAL -> REPORT_SEXUAL_ALERT_TITLE;
            case ABUSE_HATE_HARASSMENT -> REPORT_ABUSE_HATE_HARASSMENT_ALERT_TITLE;
            case SPAM_AD -> REPORT_SPAM_AD_ALERT_TITLE;
            case IMPERSONATION -> REPORT_IMPERSONATION_ALERT_TITLE;
            case SELF_HARM_DANGER -> REPORT_SELF_HARM_DANGER_ALERT_TITLE;
            case ETC -> REPORT_ETC_ALERT_TITLE;
        };
    }

    private void saveAdminAlert(AdminAlert alert) {
        adminAlertRepository.save(alert);
    }
}
