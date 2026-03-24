package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RejectAdminAlertServiceImpl 단위 테스트")
class RejectAdminAlertServiceImplTest {

    @InjectMocks
    private RejectAdminAlertServiceImpl service;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private TradeCancelRepository tradeCancelRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private void givenAdmin(Member admin, MemberDetail adminDetail) {
        when(memberUtil.getCurrentMember()).thenReturn(admin);
        when(memberDetailRepository.findById(admin.getId())).thenReturn(Optional.of(adminDetail));
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("SIGN_UP 타입 거절 시")
        class Context_with_sign_up_reject {

            @Test
            @DisplayName("신청자의 MemberDetail을 삭제한다")
            void it_deletes_member_detail() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                givenAdmin(admin, adminDetail);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(2L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getType()).thenReturn(AlertType.SIGN_UP);
                when(alert.getRequester()).thenReturn(requester);

                MemberDetail requesterDetail = mock(MemberDetail.class);

                when(adminAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(requesterDetail));

                service.execute(10L);

                verify(adminAlertRepository).delete(alert);
                verify(memberDetailRepository).delete(requesterDetail);
            }
        }

        @Nested
        @DisplayName("REPORT 타입 거절 시")
        class Context_with_report_reject {

            @Test
            @DisplayName("신고자에게 거절 알림 이벤트를 발행한다")
            void it_publishes_report_reject_event() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                givenAdmin(admin, adminDetail);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(2L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getType()).thenReturn(AlertType.REPORT);
                when(alert.getRequester()).thenReturn(requester);
                when(alert.getSourceId()).thenReturn(5L);

                MemberDetail requesterDetail = mock(MemberDetail.class);

                Member reporter = mock(Member.class);
                when(reporter.getId()).thenReturn(3L);
                Report report = mock(Report.class);
                when(report.getId()).thenReturn(5L);
                when(report.getReporter()).thenReturn(reporter);

                when(adminAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(requesterDetail));
                when(reportRepository.findById(5L)).thenReturn(Optional.of(report));

                service.execute(10L);

                verify(adminAlertRepository).delete(alert);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }
        }

        @Nested
        @DisplayName("TRADE_CANCEL 타입 거절 시")
        class Context_with_trade_cancel_reject {

            @Test
            @DisplayName("거래 취소 상태를 REJECTED로 변경하고 이벤트를 발행한다")
            void it_rejects_trade_cancel_and_publishes_event() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                givenAdmin(admin, adminDetail);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(2L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getType()).thenReturn(AlertType.TRADE_CANCEL);
                when(alert.getRequester()).thenReturn(requester);
                when(alert.getSourceId()).thenReturn(7L);

                MemberDetail requesterDetail = mock(MemberDetail.class);

                Member cancelMember = mock(Member.class);
                when(cancelMember.getId()).thenReturn(2L);
                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getId()).thenReturn(7L);
                when(tradeCancel.getMember()).thenReturn(cancelMember);

                when(adminAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(requesterDetail));
                when(tradeCancelRepository.findById(7L)).thenReturn(Optional.of(tradeCancel));

                service.execute(10L);

                verify(tradeCancel).updateStatus(TradeCancelStatus.REJECTED);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }
        }

        @Nested
        @DisplayName("AlertId에 해당하는 알림이 없을 때")
        class Context_with_alert_not_found {

            @Test
            @DisplayName("NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_admin_alert_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);
                givenAdmin(admin, adminDetail);

                when(adminAlertRepository.findById(10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }
    }
}
