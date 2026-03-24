package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundPendingMemberException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAdminAlertServiceImpl 단위 테스트")
class CreateAdminAlertServiceImplTest {

    @InjectMocks
    private CreateAdminAlertServiceImpl service;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TradeCancelRepository tradeCancelRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("REPORT 타입 알림 생성 시")
        class Context_with_report_type {

            @Test
            @DisplayName("신고 타입에 맞는 제목으로 AdminAlert를 저장한다")
            void it_saves_report_alert() {
                Report report = mock(Report.class);
                Member member = mock(Member.class);
                when(report.getReportType()).thenReturn(ReportType.SEXUAL);
                when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(member));

                service.execute(AlertType.REPORT, 1L, 2L);

                ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
                verify(adminAlertRepository).save(captor.capture());
                assertThat(captor.getValue().getTitle()).isEqualTo("음란/성적 콘텐츠 신고");
                assertThat(captor.getValue().getType()).isEqualTo(AlertType.REPORT);
                assertThat(captor.getValue().getSourceId()).isEqualTo(1L);
                assertThat(captor.getValue().getRequester()).isEqualTo(member);
            }

            @Test
            @DisplayName("Report가 없으면 NotFoundReportException을 던진다")
            void it_throws_not_found_report_exception() {
                when(reportRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(AlertType.REPORT, 1L, 2L))
                        .isInstanceOf(NotFoundReportException.class);
            }

            @Test
            @DisplayName("Member가 없으면 NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Report report = mock(Report.class);
                when(report.getReportType()).thenReturn(ReportType.SPAM_AD);
                when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
                when(memberRepository.findById(2L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(AlertType.REPORT, 1L, 2L))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("SIGN_UP 타입 알림 생성 시")
        class Context_with_sign_up_type {

            @Test
            @DisplayName("PENDING 상태의 회원으로 AdminAlert를 저장한다")
            void it_saves_sign_up_alert() {
                Member signUpMember = mock(Member.class);
                when(memberRepository.findByStatusAndId(MemberStatus.PENDING, 1L))
                        .thenReturn(Optional.of(signUpMember));

                service.execute(AlertType.SIGN_UP, 1L, null);

                ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
                verify(adminAlertRepository).save(captor.capture());
                assertThat(captor.getValue().getTitle()).isEqualTo("새로운 회원가입 요청");
                assertThat(captor.getValue().getType()).isEqualTo(AlertType.SIGN_UP);
            }

            @Test
            @DisplayName("PENDING 상태의 회원이 없으면 NotFoundPendingMemberException을 던진다")
            void it_throws_not_found_pending_member_exception() {
                when(memberRepository.findByStatusAndId(MemberStatus.PENDING, 1L))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(AlertType.SIGN_UP, 1L, null))
                        .isInstanceOf(NotFoundPendingMemberException.class);
            }
        }

        @Nested
        @DisplayName("TRADE_CANCEL 타입 알림 생성 시")
        class Context_with_trade_cancel_type {

            @Test
            @DisplayName("거래 취소 요청으로 AdminAlert를 저장한다")
            void it_saves_trade_cancel_alert() {
                Member requester = mock(Member.class);
                TradeCancel tradeCancel = mock(TradeCancel.class);
                when(tradeCancel.getMember()).thenReturn(requester);
                when(tradeCancelRepository.findByIdWithMember(1L)).thenReturn(Optional.of(tradeCancel));

                service.execute(AlertType.TRADE_CANCEL, 1L, null);

                ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
                verify(adminAlertRepository).save(captor.capture());
                assertThat(captor.getValue().getTitle()).isEqualTo("거래 철회 요청");
                assertThat(captor.getValue().getType()).isEqualTo(AlertType.TRADE_CANCEL);
                assertThat(captor.getValue().getRequester()).isEqualTo(requester);
            }

            @Test
            @DisplayName("TradeCancel이 없으면 NotFoundTradeCancelException을 던진다")
            void it_throws_not_found_trade_cancel_exception() {
                when(tradeCancelRepository.findByIdWithMember(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(AlertType.TRADE_CANCEL, 1L, null))
                        .isInstanceOf(NotFoundTradeCancelException.class);
            }
        }
    }
}
