package team.startup.gwangsan.domain.member.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.QMember;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.custom.MemberCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;

@Repository
@RequiredArgsConstructor
public class MemberCustomRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Member> findByStatusAndPlaces(MemberStatus status, List<Place> places) {
        QMember recommender = new QMember("recommender");

        return queryFactory
                .selectFrom(member).distinct()
                .join(memberDetail).on(member.id.eq(memberDetail.member.id)).fetchJoin()
                .join(member.recommender, recommender).fetchJoin()
                .where(
                        member.status.eq(status),
                        memberDetail.place.in(places)
                )
                .fetch();
    }
}
