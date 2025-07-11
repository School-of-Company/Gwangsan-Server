package team.startup.gwangsan.domain.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateProductReportRequest;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.report.service.CreateProductReportService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductReportServiceImpl implements CreateProductReportService {

    private final ProductRepository productRepository;
    private final ReportRepository reportRepository;
    private final MemberUtil memberUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(CreateProductReportRequest request) {
        Member reporter = memberUtil.getCurrentMember();

        Product product = productRepository.findById(request.productId())
                .orElseThrow(NotFoundProductException::new);

        Member reported = product.getMember();

        if (reported.getId().equals(reporter.getId())) {
            throw new SelfReportNotAllowedException();
        }

        reportRepository.findByReporterAndReportedAndContent(reporter, reported, request.content())
                .ifPresent(r -> { throw new AlreadyReportedException(); });

        Report report = Report.builder()
                .reportType(request.reportType())
                .content(request.content())
                .reporter(reporter)
                .reported(reported)
                .build();

        reportRepository.save(report);

        eventPublisher.publishEvent(
                new CreateAdminAlertEvent(
                        AlertType.REPORT,
                        report.getId(),
                        reporter
                )
        );
    }
}
