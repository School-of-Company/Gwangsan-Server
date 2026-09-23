package team.startup.gwangsan.domain.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
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
import team.startup.gwangsan.domain.report.service.CreateReportService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateReportServiceImpl implements CreateReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final ReportImageRepository reportImageRepository;
    private final MemberUtil memberUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(CreateReportRequest request) {
        Member reporter = memberUtil.getCurrentMember();

        // sourceId 는 targetType 없이는 해석할 수 없다. 게시글 신고를 회원 ID 로 조회하면
        // 실패하거나, 우연히 같은 숫자의 회원이 있을 때 엉뚱한 회원이 신고 대상으로 기록된다.
        Product product = request.targetTypeOrMember() == ReportTargetType.PRODUCT
                ? productRepository.findActiveById(request.sourceId())
                        .orElseThrow(NotFoundProductException::new)
                : null;

        Member reported = product != null
                ? product.getMember()
                : memberRepository.findById(request.sourceId())
                        .orElseThrow(NotFoundMemberException::new);

        if (reporter.getId().equals(reported.getId())) {
            throw new SelfReportNotAllowedException();
        }

        validateNotReportedYet(reporter, reported, product, request.reportType());

        Report report = Report.builder()
                .reportType(request.reportType())
                .content(request.content())
                .reporter(reporter)
                .reported(reported)
                .product(product)
                .build();

        reportRepository.save(report);

        if (request.imageIds() != null && !request.imageIds().isEmpty()) {
            List<Image> images = imageRepository.findAllById(request.imageIds());
            List<ReportImage> reportImages = images.stream()
                    .map(image -> new ReportImage(image, report))
                    .toList();

            reportImageRepository.saveAll(reportImages);
        }

        eventPublisher.publishEvent(
                new CreateAdminAlertEvent(
                        AlertType.REPORT,
                        report.getId(),
                        reporter.getId()
                )
        );
    }

    private void validateNotReportedYet(Member reporter, Member reported, Product product, ReportType reportType) {
        Optional<Report> duplicated = product != null
                ? reportRepository.findByReporterAndProductAndReportType(reporter, product, reportType)
                : reportRepository.findByReporterAndReportedAndReportTypeAndProductIsNull(reporter, reported, reportType);

        if (duplicated.isPresent()) {
            throw new AlreadyReportedException();
        }
    }
}
