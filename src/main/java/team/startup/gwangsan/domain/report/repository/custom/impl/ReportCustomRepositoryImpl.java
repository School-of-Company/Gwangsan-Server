package team.startup.gwangsan.domain.report.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.QMember;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.repository.custom.ReportCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.report.entity.QReport.report;

@Repository
@RequiredArgsConstructor
public class ReportCustomRepositoryImpl implements ReportCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Report> findByPlaces(List<Place> places) {
        QMember reporter = new QMember("reporter");
        QMember reported = new QMember("reported");

        return queryFactory
                .selectFrom(report).distinct()
                .join(report.reporter, reporter).fetchJoin()
                .join(report.reported, reported).fetchJoin()
                .join(memberDetail).on(member.id.eq(memberDetail.member.id)).fetchJoin()
                .where(memberDetail.place.in(places))
                .fetch();
    }
}
