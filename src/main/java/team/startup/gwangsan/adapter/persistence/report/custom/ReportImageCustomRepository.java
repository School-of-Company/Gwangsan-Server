package team.startup.gwangsan.adapter.persistence.report.custom;

import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaEntity;

import java.util.Collection;
import java.util.List;

public interface ReportImageCustomRepository {
    List<ReportImageJpaEntity> findAllByReportIdIn(Collection<Long> reportIds);

}
