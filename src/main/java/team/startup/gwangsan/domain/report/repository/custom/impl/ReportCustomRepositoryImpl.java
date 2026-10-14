package team.startup.gwangsan.domain.report.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.repository.custom.ReportCustomRepository;

import java.util.List;

import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import static team.startup.gwangsan.domain.report.entity.QReport.report;

@Repository
@RequiredArgsConstructor
public class ReportCustomRepositoryImpl implements ReportCustomRepository {
    private static final QMemberJpaEntity member = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Report> findByPlaces(List<Place> places) {
        QMemberJpaEntity reporter = new QMemberJpaEntity("reporter");
        QMemberJpaEntity reported = new QMemberJpaEntity("reported");

        return queryFactory
                .selectFrom(report).distinct()
                .join(report.reporter, reporter).fetchJoin()
                .join(report.reported, reported).fetchJoin()
                .join(memberDetail).on(reporter.id.eq(memberDetail.member.id)).fetchJoin()
                .where(memberDetail.place.in(places))
                .fetch();
    }
}
