package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.RejectAdminAlertService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RejectAdminAlertServiceImpl implements RejectAdminAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final MemberRepository memberRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final ReportRepository reportRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;

    @Override
    @Transactional
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        MemberDetail adminDetail = getMemberDetail(admin.getId());

        AdminAlert alert = getAdminAlert(alertId);
        MemberDetail alertMemberDetail = getMemberDetail(alert.getRequester().getId());

        validatePlaceUtil.validateSamePlace(admin, adminDetail, alertMemberDetail);

        adminAlertRepository.delete(alert);

        switch (alert.getType()) {
            case SIGN_UP -> rejectSignUp(alertMemberDetail);
            case TRADE_COMPLETE -> rejectTradeComplete(alert);
            case REPORT -> rejectReport(alert);
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

    private void rejectTradeComplete(AdminAlert alert) {
        List<Long> memberIds = List.of(
                alert.getOtherMember().getId(),
                alert.getRequester().getId()
        );
        List<TradeComplete> tradeCompletes = tradeCompleteRepository.findAllById(memberIds);
        tradeCompleteRepository.deleteAll(tradeCompletes);
    }

    private void rejectReport(AdminAlert alert) {
        Report report = reportRepository.findById(alert.getSourceId())
                .orElseThrow(NotFoundReportException::new);
        reportRepository.delete(report);
    }

}
