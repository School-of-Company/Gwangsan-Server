package team.startup.gwangsan.domain.member.repository.custom.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.repository.custom.MemberCustomRepository;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.place.entity.QPlace.place;

@Repository
@RequiredArgsConstructor
public class MemberCustomRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<GetProductMemberResponse> findProductMemberResponsesByMemberIds(Collection<Long> memberIds) {

        return queryFactory
                .select(Projections.constructor(
                        GetProductMemberResponse.class,
                        member.id,
                        member.nickname,
                        place.name,
                        memberDetail.light
                ))
                .from(member)
                .join(memberDetail).on(memberDetail.member.id.eq(member.id))
                .join(memberDetail.place, place)
                .where(member.id.in(memberIds))
                .fetch();
    }
}
