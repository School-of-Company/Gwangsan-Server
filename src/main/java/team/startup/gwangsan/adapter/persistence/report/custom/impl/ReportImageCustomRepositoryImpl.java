package team.startup.gwangsan.adapter.persistence.report.custom.impl;

import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.report.custom.ReportImageCustomRepository;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.adapter.persistence.report.QReportImageJpaEntity.reportImageJpaEntity;

@Repository
@RequiredArgsConstructor
public class ReportImageCustomRepositoryImpl implements ReportImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReportImageJpaEntity> findAllByReportIdIn(Collection<Long> reportIds) {
        return queryFactory
                .selectFrom(reportImageJpaEntity)
                .join(reportImageJpaEntity.image).fetchJoin()
                .where(reportImageJpaEntity.report.id.in(reportIds))
                .fetch();
    }
}
