package team.startup.gwangsan.domain.report.repository.custom;

import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;

import java.util.Collection;
import java.util.List;

public interface ReportImageCustomRepository {
    List<ReportImage> findByReportIn(Collection<Report> reports);

}
