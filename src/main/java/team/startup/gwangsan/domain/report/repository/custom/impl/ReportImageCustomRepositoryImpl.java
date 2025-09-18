package team.startup.gwangsan.domain.report.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.repository.custom.ReportImageCustomRepository;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.domain.report.entity.QReportImage.reportImage;

@Repository
@RequiredArgsConstructor
public class ReportImageCustomRepositoryImpl implements ReportImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReportImage> findByReportIn(Collection<Report> reports) {
        return queryFactory
                .selectFrom(reportImage)
                .join(reportImage.image).fetchJoin()
                .where(reportImage.report.in(reports))
                .fetch();
    }
}
