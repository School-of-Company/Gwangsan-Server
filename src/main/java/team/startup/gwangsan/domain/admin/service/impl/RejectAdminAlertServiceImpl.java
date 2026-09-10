package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.RejectAdminAlertService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class RejectAdminAlertServiceImpl implements RejectAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final ReportRepository reportRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final TradeCancelRepository tradeCancelRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 잠금과 격리 수준의 이유는 {@code TradeCancelServiceImpl} 주석 참고. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = getMemberDetail(admin.getId());

        AdminAlert alert = getAdminAlert(alertId);
        MemberDetail alertMemberDetail = getMemberDetail(alert.getRequester().getId());

        validatePlaceUtil.validateSamePlace(admin, adminDetail, alertMemberDetail);

        // 기각도 철회 요청·승인·자동 철회와 같은 잠금을 거쳐야 한다. 그러지 않으면
        // 이미 승인된(APPROVED) 철회를 REJECTED 로 덮어써 광산과 상태가 어긋난다.
        if (alert.getType() == team.startup.gwangsan.domain.admin.entity.constant.AlertType.TRADE_CANCEL) {
            Long productId = tradeCancelRepository.findProductIdById(alert.getSourceId())
                    .orElseThrow(NotFoundTradeCancelException::new);
            productRepository.findByIdForUpdate(productId)
                    .orElseThrow(NotFoundProductException::new);
        }

        adminAlertRepository.delete(alert);

        switch (alert.getType()) {
            case SIGN_UP -> rejectSignUp(alertMemberDetail);
            case REPORT -> rejectReport(alert);
            case TRADE_CANCEL -> rejectTradeCancel(alert);
        }
    }

    private MemberDetail getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }

    private AdminAlert getAdminAlert(Long alertId) {
        return adminAlertRepository.findById(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);
    }

    private void rejectSignUp(MemberDetail memberDetail) {
        memberDetailRepository.delete(memberDetail);
    }

    private void rejectReport(AdminAlert alert) {
        Report report = reportRepository.findById(alert.getSourceId())
                .orElseThrow(NotFoundReportException::new);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                report.getId(),
                report.getReporter().getId(),
                AlertType.REPORT_REJECT
        ));
    }

    private void rejectTradeCancel(AdminAlert alert) {
        TradeCancel tradeCancel = tradeCancelRepository.findById(alert.getSourceId())
                .orElseThrow(NotFoundTradeCancelException::new);

        if (tradeCancel.getStatus() != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }

        tradeCancel.updateStatus(TradeCancelStatus.REJECTED);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                tradeCancel.getId(),
                tradeCancel.getMember().getId(),
                AlertType.TRADE_CANCEL_REJECT
        ));
    }
}
