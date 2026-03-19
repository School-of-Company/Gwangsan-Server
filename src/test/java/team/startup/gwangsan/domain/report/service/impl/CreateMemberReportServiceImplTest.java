package team.startup.gwangsan.domain.report.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateMemberReportRequest;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductReportServiceImpl 단위 테스트")
class CreateMemberReportServiceImplTest {

    @Mock private MemberUtil memberUtil;
    @Mock private MemberRepository memberRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private ReportImageRepository reportImageRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateMemberReportServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @ParameterizedTest(name = "reportType={0} 일 때 회원 기반으로 신고를 저장한다")
        @EnumSource(value = ReportType.class, names = {"SEXUAL", "ABUSE_HATE_HARASSMENT", "SPAM_AD", "IMPERSONATION", "SELF_HARM_DANGER"})
        @DisplayName("회원 기반 타입은 신고 대상 회원을 조회하고 저장한다")
        void it_saves_report_with_member_based_types(ReportType reportType) {
            // given
            Member reporter = mock(Member.class);
            when(reporter.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mock(Member.class);
            when(reported.getId()).thenReturn(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportType(reporter, reported, reportType))
                    .thenReturn(Optional.empty());

            CreateMemberReportRequest request = new CreateMemberReportRequest(2L, reportType, "신고 내용", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            Report saved = captor.getValue();
            assertThat(saved.getReporter()).isEqualTo(reporter);
            assertThat(saved.getReported()).isEqualTo(reported);
            assertThat(saved.getReportType()).isEqualTo(reportType);
            assertThat(saved.getContent()).isEqualTo("신고 내용");
            verify(eventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
        }

        @Test
        @DisplayName("ETC 타입은 reported=null 로 저장한다")
        void it_saves_report_with_null_reported_when_etc() {
            // given
            Member reporter = mock(Member.class);
            when(reporter.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            CreateMemberReportRequest request = new CreateMemberReportRequest(null, ReportType.ETC, "기타 신고", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            assertThat(captor.getValue().getReported()).isNull();
            verify(eventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
        }

        @Test
        @DisplayName("이미지 ID가 있으면 ReportImage 를 저장한다")
        void it_saves_report_images_when_image_ids_exist() {
            // given
            Member reporter = mock(Member.class);
            when(reporter.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mock(Member.class);
            when(reported.getId()).thenReturn(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));
            when(reportRepository.findByReporterAndReportedAndReportType(reporter, reported, ReportType.SEXUAL))
                    .thenReturn(Optional.empty());

            Image image1 = mock(Image.class);
            Image image2 = mock(Image.class);
            when(imageRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(image1, image2));

            CreateMemberReportRequest request = new CreateMemberReportRequest(2L, ReportType.SEXUAL, "신고", List.of(10L, 20L));

            ArgumentCaptor<List<ReportImage>> captor = ArgumentCaptor.forClass(List.class);

            // when
            service.execute(request);

            // then
            verify(reportImageRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("본인 신고 시 SelfReportNotAllowedException 을 던진다")
        void it_throws_when_self_report() {
            // given
            Member reporter = mock(Member.class);
            when(reporter.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mock(Member.class);
            when(reported.getId()).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(reported));

            CreateMemberReportRequest request = new CreateMemberReportRequest(1L, ReportType.SPAM_AD, "신고", null);

            // when & then
            assertThrows(SelfReportNotAllowedException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 신고한 대상이면 AlreadyReportedException 을 던진다")
        void it_throws_when_already_reported() {
            // given
            Member reporter = mock(Member.class);
            when(reporter.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mock(Member.class);
            when(reported.getId()).thenReturn(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportType(reporter, reported, ReportType.IMPERSONATION))
                    .thenReturn(Optional.of(mock(Report.class)));

            CreateMemberReportRequest request = new CreateMemberReportRequest(2L, ReportType.IMPERSONATION, "신고", null);

            // when & then
            assertThrows(AlreadyReportedException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 회원 신고 시 NotFoundMemberException 을 던진다")
        void it_throws_when_member_not_found() {
            // given
            Member reporter = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            CreateMemberReportRequest request = new CreateMemberReportRequest(999L, ReportType.ABUSE_HATE_HARASSMENT, "신고", null);

            // when & then
            assertThrows(NotFoundMemberException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }
    }
}
