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
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.exception.InvalidReportTypeException;
import team.startup.gwangsan.domain.report.exception.SelfReportNotAllowedException;
import team.startup.gwangsan.domain.report.presentation.dto.request.CreateProductReportRequest;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.report.service.CreateProductReportService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateProductReportServiceImpl implements CreateProductReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ReportImageRepository reportImageRepository;
    private final MemberUtil memberUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(CreateProductReportRequest request) {
        Member reporter = memberUtil.getCurrentMember();
        Member reported;

        switch (request.reportType()) {
            case SEXUAL, ABUSE_HATE_HARASSMENT, SPAM_AD, IMPERSONATION, SELF_HARM_DANGER -> {
                reported = memberRepository.findById(request.sourceId())
                        .orElseThrow(NotFoundMemberException::new);
            }

            case ETC -> {
                reported = null;
            }

            default -> throw new InvalidReportTypeException();
        }

        if (reported != null && reporter.getId().equals(reported.getId())) {
            throw new SelfReportNotAllowedException();
        }

        if (reported != null) {
            reportRepository.findByReporterAndReportedAndReportType(reporter, reported, request.reportType())
                    .ifPresent(report -> {
                        throw new AlreadyReportedException();
                    });
        }

        Report report = Report.builder()
                .reportType(request.reportType())
                .content(request.content())
                .reporter(reporter)
                .reported(reported)
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
}
