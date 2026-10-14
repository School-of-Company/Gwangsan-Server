package team.startup.gwangsan.application.report.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.report.data.CreateReportCommand;
import team.startup.gwangsan.application.report.port.out.LoadReportImagePort;
import team.startup.gwangsan.application.report.port.out.LoadReportPort;
import team.startup.gwangsan.application.report.port.out.LoadReportTargetPort;
import team.startup.gwangsan.application.report.port.out.RequestReportAlertPort;
import team.startup.gwangsan.application.report.port.out.SaveReportPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;
import team.startup.gwangsan.domain.report.model.Report;
import team.startup.gwangsan.domain.report.model.ReportTargetType;
import team.startup.gwangsan.domain.report.model.ReportType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateReportService 단위 테스트")
class CreateReportServiceTest {

    @Mock private LoadReportTargetPort loadReportTargetPort;
    @Mock private LoadReportPort loadReportPort;
    @Mock private SaveReportPort saveReportPort;
    @Mock private LoadReportImagePort loadReportImagePort;
    @Mock private RequestReportAlertPort requestReportAlertPort;

    @InjectMocks
    private CreateReportService service;

    private static CreateReportCommand command(ReportTargetType targetType, Long sourceId, ReportType type, List<Long> imageIds) {
        return new CreateReportCommand(1L, targetType, sourceId, type, "신고 내용", imageIds);
    }

    private Report savedReport() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(saveReportPort).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("execute() 메서드는 (회원 신고)")
    class Describe_execute_member {

        @ParameterizedTest(name = "reportType={0} 일 때 회원 기반으로 신고를 저장한다")
        @EnumSource(ReportType.class)
        @DisplayName("모든 타입은 신고 대상 회원을 확인하고 저장한 뒤 관리자 알림을 요청한다")
        void it_saves_member_report(ReportType reportType) {
            when(saveReportPort.save(any())).thenReturn(100L);

            service.execute(command(ReportTargetType.MEMBER, 2L, reportType, null));

            Report saved = savedReport();
            assertThat(saved).isEqualTo(new Report(1L, 2L, null, reportType, "신고 내용"));
            verify(loadReportTargetPort).checkMemberExists(2L);
            verify(loadReportPort).existsMemberReport(1L, 2L, reportType);
            verify(requestReportAlertPort).requestAdminAlert(100L, 1L);
        }

        @Test
        @DisplayName("이미지 ID가 있으면 존재하는 이미지만 신고 이미지로 저장한 뒤 알림을 요청한다")
        void it_saves_existing_report_images() {
            when(saveReportPort.save(any())).thenReturn(100L);
            when(loadReportImagePort.findExistingImageIds(List.of(10L, 20L, 30L))).thenReturn(List.of(10L, 20L));

            service.execute(command(ReportTargetType.MEMBER, 2L, ReportType.SEXUAL, List.of(10L, 20L, 30L)));

            InOrder order = inOrder(saveReportPort, requestReportAlertPort);
            order.verify(saveReportPort).save(any());
            order.verify(saveReportPort).saveImages(100L, List.of(10L, 20L));
            order.verify(requestReportAlertPort).requestAdminAlert(100L, 1L);
        }

        @Test
        @DisplayName("이미지 ID가 빈 목록이면 신고와 관리자 알림은 처리하고 이미지 저장은 생략한다")
        void it_skips_images_when_image_ids_are_empty() {
            when(saveReportPort.save(any())).thenReturn(100L);

            service.execute(command(ReportTargetType.MEMBER, 2L, ReportType.ETC, List.of()));

            verify(saveReportPort, never()).saveImages(anyLong(), any());
            verifyNoInteractions(loadReportImagePort);
            verify(requestReportAlertPort).requestAdminAlert(100L, 1L);
        }

        @Test
        @DisplayName("본인 신고 시 SelfReportNotAllowedException 을 던지고 중복 검사·저장을 하지 않는다")
        void it_throws_when_self_report() {
            assertThatThrownBy(() -> service.execute(command(ReportTargetType.MEMBER, 1L, ReportType.SPAM_AD, null)))
                    .isInstanceOf(SelfReportNotAllowedException.class);

            verifyNoInteractions(loadReportPort, saveReportPort, requestReportAlertPort);
        }

        @Test
        @DisplayName("이미 신고한 대상이면 AlreadyReportedException 을 던진다")
        void it_throws_when_already_reported() {
            when(loadReportPort.existsMemberReport(1L, 2L, ReportType.IMPERSONATION)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command(ReportTargetType.MEMBER, 2L, ReportType.IMPERSONATION, null)))
                    .isInstanceOf(AlreadyReportedException.class);

            verifyNoInteractions(saveReportPort, requestReportAlertPort);
        }

        @Test
        @DisplayName("존재하지 않는 회원 신고 시 NotFoundMemberException 을 그대로 던진다")
        void it_throws_when_member_not_found() {
            doThrow(new NotFoundMemberException()).when(loadReportTargetPort).checkMemberExists(999L);

            assertThatThrownBy(() -> service.execute(command(ReportTargetType.MEMBER, 999L, ReportType.ETC, null)))
                    .isInstanceOf(NotFoundMemberException.class);

            verifyNoInteractions(loadReportPort, saveReportPort);
        }

        @Test
        @DisplayName("회원 신고는 게시글 신고 중복 검사를 쓰지 않는다")
        void it_uses_member_duplicate_check_only() {
            when(saveReportPort.save(any())).thenReturn(100L);

            service.execute(command(ReportTargetType.MEMBER, 2L, ReportType.SPAM_AD, null));

            verify(loadReportPort, never()).existsProductReport(any(), any(), any());
            verify(loadReportTargetPort, never()).loadActiveProductOwnerId(any());
        }
    }

    @Nested
    @DisplayName("execute() 메서드는 (게시글 신고)")
    class Describe_execute_product {

        @Test
        @DisplayName("게시글과 작성자를 함께 신고 대상으로 저장한다")
        void it_saves_report_with_product_and_owner() {
            when(loadReportTargetPort.loadActiveProductOwnerId(5L)).thenReturn(2L);
            when(saveReportPort.save(any())).thenReturn(100L);

            service.execute(command(ReportTargetType.PRODUCT, 5L, ReportType.SPAM_AD, null));

            assertThat(savedReport()).isEqualTo(new Report(1L, 2L, 5L, ReportType.SPAM_AD, "신고 내용"));
            verify(loadReportPort).existsProductReport(1L, 5L, ReportType.SPAM_AD);
            verify(loadReportPort, never()).existsMemberReport(any(), any(), any());
            verify(requestReportAlertPort).requestAdminAlert(100L, 1L);
        }

        @Test
        @DisplayName("게시글 ID와 같은 숫자의 회원이 있어도 그 회원을 신고 대상으로 삼지 않는다")
        void it_does_not_report_member_whose_id_equals_product_id() {
            when(loadReportTargetPort.loadActiveProductOwnerId(5L)).thenReturn(2L);

            service.execute(command(ReportTargetType.PRODUCT, 5L, ReportType.SEXUAL, null));

            verify(loadReportTargetPort, never()).checkMemberExists(any());
            assertThat(savedReport().reportedId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("본인 게시글 신고 시 SelfReportNotAllowedException 을 던진다")
        void it_throws_when_reporting_own_product() {
            when(loadReportTargetPort.loadActiveProductOwnerId(5L)).thenReturn(1L);

            assertThatThrownBy(() -> service.execute(command(ReportTargetType.PRODUCT, 5L, ReportType.ETC, null)))
                    .isInstanceOf(SelfReportNotAllowedException.class);

            verifyNoInteractions(saveReportPort);
        }

        @Test
        @DisplayName("같은 게시글을 같은 타입으로 다시 신고하면 AlreadyReportedException 을 던진다")
        void it_throws_already_reported_for_same_product_and_type() {
            when(loadReportTargetPort.loadActiveProductOwnerId(5L)).thenReturn(2L);
            when(loadReportPort.existsProductReport(1L, 5L, ReportType.SPAM_AD)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(command(ReportTargetType.PRODUCT, 5L, ReportType.SPAM_AD, null)))
                    .isInstanceOf(AlreadyReportedException.class);

            verifyNoInteractions(saveReportPort);
        }

        @Test
        @DisplayName("삭제된 게시글 신고 시 NotFoundProductException 을 그대로 던진다")
        void it_throws_not_found_product_exception_when_product_deleted() {
            when(loadReportTargetPort.loadActiveProductOwnerId(5L)).thenThrow(new NotFoundProductException());

            assertThatThrownBy(() -> service.execute(command(ReportTargetType.PRODUCT, 5L, ReportType.ETC, null)))
                    .isInstanceOf(NotFoundProductException.class);

            verifyNoInteractions(saveReportPort);
        }
    }
}
