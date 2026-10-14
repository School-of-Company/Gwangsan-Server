package team.startup.gwangsan.adapter.persistence.report.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.custom.ReportCustomRepository;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;

import static team.startup.gwangsan.adapter.persistence.report.QReportJpaEntity.reportJpaEntity;

@Repository
@RequiredArgsConstructor
public class ReportCustomRepositoryImpl implements ReportCustomRepository {
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReportJpaEntity> findByPlaces(List<PlaceJpaEntity> places) {
        QMemberJpaEntity reporter = new QMemberJpaEntity("reporter");
        QMemberJpaEntity reported = new QMemberJpaEntity("reported");

        return queryFactory
                .selectFrom(reportJpaEntity).distinct()
                .join(reportJpaEntity.reporter, reporter).fetchJoin()
                .join(reportJpaEntity.reported, reported).fetchJoin()
                .join(memberDetail).on(reporter.id.eq(memberDetail.member.id)).fetchJoin()
                .where(memberDetail.place.in(places))
                .fetch();
    }
}
