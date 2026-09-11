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
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.entity.constant.ReportTargetType;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateReportRequest;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateReportServiceImpl 단위 테스트")
class CreateReportServiceImplTest {

    @Mock private MemberUtil memberUtil;
    @Mock private MemberRepository memberRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private ReportImageRepository reportImageRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateReportServiceImpl service;

    private Member mockMember(Long id) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    @Nested
    @DisplayName("execute() 메서드는 (회원 신고)")
    class Describe_execute_member {

        @ParameterizedTest(name = "reportType={0} 일 때 회원 기반으로 신고를 저장한다")
        @EnumSource(ReportType.class)
        @DisplayName("모든 타입은 신고 대상 회원을 조회하고 저장한다")
        void it_saves_report_with_member_based_types(ReportType reportType) {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, reportType))
                    .thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.MEMBER, 2L, reportType, "신고 내용", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            Report saved = captor.getValue();
            assertThat(saved.getReporter()).isEqualTo(reporter);
            assertThat(saved.getReported()).isEqualTo(reported);
            assertThat(saved.getProduct()).isNull();
            assertThat(saved.getReportType()).isEqualTo(reportType);
            assertThat(saved.getContent()).isEqualTo("신고 내용");
            verify(eventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
        }

        @Test
        @DisplayName("targetType 이 없는 기존 요청은 회원 신고로 해석한다")
        void it_treats_null_target_type_as_member() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, ReportType.ETC))
                    .thenReturn(Optional.empty());

            CreateReportRequest request = new CreateReportRequest(null, 2L, ReportType.ETC, "기타 신고", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            assertThat(captor.getValue().getReported()).isEqualTo(reported);
            assertThat(captor.getValue().getProduct()).isNull();
            verify(productRepository, never()).findActiveById(any());
            verify(eventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
        }

        @Test
        @DisplayName("이미지 ID가 있으면 ReportImage 를 저장한다")
        void it_saves_report_images_when_image_ids_exist() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));
            when(reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, ReportType.SEXUAL))
                    .thenReturn(Optional.empty());

            Image image1 = mock(Image.class);
            Image image2 = mock(Image.class);
            when(imageRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(image1, image2));

            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.MEMBER, 2L, ReportType.SEXUAL, "신고", List.of(10L, 20L));

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
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(reported));

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.MEMBER, 1L, ReportType.SPAM_AD, "신고", null);

            // when & then
            assertThrows(SelfReportNotAllowedException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 신고한 대상이면 AlreadyReportedException 을 던진다")
        void it_throws_when_already_reported() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, ReportType.IMPERSONATION))
                    .thenReturn(Optional.of(mock(Report.class)));

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.MEMBER, 2L, ReportType.IMPERSONATION, "신고", null);

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

            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.MEMBER, 999L, ReportType.ABUSE_HATE_HARASSMENT, "신고", null);

            // when & then
            assertThrows(NotFoundMemberException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 작성자의 게시글을 이미 신고했어도 회원 신고는 막히지 않는다")
        void it_allows_member_report_even_if_product_report_exists() {
            // given - 게시글 신고 이력(product 가 채워진 행)은 회원 신고 중복 검사 대상이 아니다
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member reported = mockMember(2L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(reported));

            when(reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, ReportType.SPAM_AD))
                    .thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.MEMBER, 2L, ReportType.SPAM_AD, "신고", null);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(any(Report.class));
            verify(reportRepository, never()).findByReporterAndProductAndReportType(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("execute() 메서드는 (게시글 신고)")
    class Describe_execute_product {

        @Test
        @DisplayName("게시글과 작성자를 함께 신고 대상으로 저장한다")
        void it_saves_report_with_product_and_owner() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member owner = mockMember(2L);
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findActiveById(5L)).thenReturn(Optional.of(product));

            when(reportRepository.findByReporterAndProductAndReportType(reporter, product, ReportType.SPAM_AD))
                    .thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 5L, ReportType.SPAM_AD, "광고 게시글", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            Report saved = captor.getValue();
            assertThat(saved.getProduct()).isEqualTo(product);
            assertThat(saved.getReported()).isEqualTo(owner);
            assertThat(saved.getReporter()).isEqualTo(reporter);
            verify(eventPublisher).publishEvent(any(CreateAdminAlertEvent.class));
        }

        @Test
        @DisplayName("게시글 ID와 같은 숫자의 회원이 있어도 그 회원을 신고 대상으로 삼지 않는다")
        void it_does_not_report_member_whose_id_equals_product_id() {
            // given - 이슈 #392 의 핵심: sourceId 5 는 회원 5 가 아니라 게시글 5 로만 해석돼야 한다
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member owner = mockMember(2L);
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findActiveById(5L)).thenReturn(Optional.of(product));

            when(reportRepository.findByReporterAndProductAndReportType(reporter, product, ReportType.SEXUAL))
                    .thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 5L, ReportType.SEXUAL, "신고", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(memberRepository, never()).findById(any());
            verify(reportRepository).save(captor.capture());
            assertThat(captor.getValue().getReported()).isEqualTo(owner);
            assertThat(captor.getValue().getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("본인 게시글 신고 시 SelfReportNotAllowedException 을 던진다")
        void it_throws_self_report_not_allowed_when_reporting_own_product() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member owner = mockMember(1L);
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findActiveById(5L)).thenReturn(Optional.of(product));

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 5L, ReportType.ETC, "신고", null);

            // when & then
            assertThrows(SelfReportNotAllowedException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 게시글을 같은 타입으로 다시 신고하면 AlreadyReportedException 을 던진다")
        void it_throws_already_reported_for_same_product_and_type() {
            // given
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member owner = mockMember(2L);
            Product product = mock(Product.class);
            when(product.getMember()).thenReturn(owner);
            when(productRepository.findActiveById(5L)).thenReturn(Optional.of(product));

            when(reportRepository.findByReporterAndProductAndReportType(reporter, product, ReportType.SPAM_AD))
                    .thenReturn(Optional.of(mock(Report.class)));

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 5L, ReportType.SPAM_AD, "신고", null);

            // when & then
            assertThrows(AlreadyReportedException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 작성자의 다른 게시글은 별개 신고로 허용한다")
        void it_allows_report_for_another_product_of_same_owner() {
            // given - 게시글 6 은 신고 이력이 없으므로 게시글 5 신고와 무관하게 저장돼야 한다
            Member reporter = mockMember(1L);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);

            Member owner = mockMember(2L);
            Product anotherProduct = mock(Product.class);
            when(anotherProduct.getMember()).thenReturn(owner);
            when(productRepository.findActiveById(6L)).thenReturn(Optional.of(anotherProduct));

            when(reportRepository.findByReporterAndProductAndReportType(reporter, anotherProduct, ReportType.SPAM_AD))
                    .thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 6L, ReportType.SPAM_AD, "신고", null);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);

            // when
            service.execute(request);

            // then
            verify(reportRepository).save(captor.capture());
            assertThat(captor.getValue().getProduct()).isEqualTo(anotherProduct);
        }

        @Test
        @DisplayName("삭제된 게시글 신고 시 NotFoundProductException 을 던진다")
        void it_throws_not_found_product_exception_when_product_deleted() {
            // given - findActiveById 는 DELETED 상태 게시글을 제외한다
            Member reporter = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(reporter);
            when(productRepository.findActiveById(5L)).thenReturn(Optional.empty());

            CreateReportRequest request =
                    new CreateReportRequest(ReportTargetType.PRODUCT, 5L, ReportType.ETC, "신고", null);

            // when & then
            assertThrows(NotFoundProductException.class, () -> service.execute(request));
            verify(reportRepository, never()).save(any());
        }
    }
}
