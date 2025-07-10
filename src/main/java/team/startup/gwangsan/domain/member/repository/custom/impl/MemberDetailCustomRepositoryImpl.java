package team.startup.gwangsan.domain.member.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;

@Repository
@RequiredArgsConstructor
public class MemberDetailCustomRepositoryImpl implements MemberDetailCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Place findPlaceByMemberId(Long id) {
        return Optional.ofNullable(queryFactory
                .select(memberDetail.place)
                .from(memberDetail)
                .where(memberDetail.id.eq(id))
                .fetchOne())
                .orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public List<MemberDetail> findAllWithMember() {
        return queryFactory
                .selectFrom(memberDetail)
                .join(memberDetail.member).fetchJoin()
                .fetch();
    }

    @Override
    public Map<Long, String> findPlaceNameMapByMemberIds(Set<Long> memberIds) {
        return queryFactory
                .select(Projections.tuple(
                        memberDetail.member.id,
                        memberDetail.place.name
                ))
                .from(memberDetail)
                .where(memberDetail.member.id.in(memberIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(memberDetail.member.id),
                        tuple -> tuple.get(memberDetail.place.name)
                ));
    }

}
