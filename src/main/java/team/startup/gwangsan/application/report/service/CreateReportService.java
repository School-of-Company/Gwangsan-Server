package team.startup.gwangsan.application.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.report.data.CreateReportCommand;
import team.startup.gwangsan.application.report.port.in.CreateReportUseCase;
import team.startup.gwangsan.application.report.port.out.LoadReportImagePort;
import team.startup.gwangsan.application.report.port.out.LoadReportPort;
import team.startup.gwangsan.application.report.port.out.LoadReportTargetPort;
import team.startup.gwangsan.application.report.port.out.RequestReportAlertPort;
import team.startup.gwangsan.application.report.port.out.SaveReportPort;
import team.startup.gwangsan.domain.report.exception.AlreadyReportedException;
import team.startup.gwangsan.domain.report.model.Report;
import team.startup.gwangsan.domain.report.model.ReportTargetType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateReportService implements CreateReportUseCase {

    private final LoadReportTargetPort loadReportTargetPort;
    private final LoadReportPort loadReportPort;
    private final SaveReportPort saveReportPort;
    private final LoadReportImagePort loadReportImagePort;
    private final RequestReportAlertPort requestReportAlertPort;

    @Override
    @Transactional
    public void execute(CreateReportCommand command) {
        // sourceId 는 targetType 없이는 해석할 수 없다. 게시글 신고를 회원 ID 로 조회하면
        // 실패하거나, 우연히 같은 숫자의 회원이 있을 때 엉뚱한 회원이 신고 대상으로 기록된다.
        Long productId = command.targetType() == ReportTargetType.PRODUCT ? command.sourceId() : null;
        Long reportedId;
        if (productId != null) {
            reportedId = loadReportTargetPort.loadActiveProductOwnerId(productId);
        } else {
            loadReportTargetPort.checkMemberExists(command.sourceId());
            reportedId = command.sourceId();
        }

        Report report = Report.create(command.reporterId(), reportedId, productId, command.reportType(), command.content());
        validateNotReportedYet(report);

        Long reportId = saveReportPort.save(report);

        if (command.imageIds() != null && !command.imageIds().isEmpty()) {
            List<Long> imageIds = loadReportImagePort.findExistingImageIds(command.imageIds());
            saveReportPort.saveImages(reportId, imageIds);
        }

        requestReportAlertPort.requestAdminAlert(reportId, command.reporterId());
    }

    private void validateNotReportedYet(Report report) {
        boolean duplicated = report.isProductReport()
                ? loadReportPort.existsProductReport(report.reporterId(), report.productId(), report.reportType())
                : loadReportPort.existsMemberReport(report.reporterId(), report.reportedId(), report.reportType());
        if (duplicated) {
            throw new AlreadyReportedException();
        }
    }
}
