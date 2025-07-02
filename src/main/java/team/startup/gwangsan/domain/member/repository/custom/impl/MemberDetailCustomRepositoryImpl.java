package team.startup.gwangsan.domain.member.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.Optional;

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
}
