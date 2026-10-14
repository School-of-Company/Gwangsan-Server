package team.startup.gwangsan.adapter.persistence.member;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.web.post.dto.GetProductMemberResponse;

import java.util.Collection;
import java.util.List;

import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.QPlaceJpaEntity;

@Repository
@RequiredArgsConstructor
public class MemberCustomRepositoryImpl implements MemberCustomRepository {
    private static final QPlaceJpaEntity place = QPlaceJpaEntity.placeJpaEntity;

    private static final QMemberJpaEntity member = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

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
