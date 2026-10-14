package team.startup.gwangsan.adapter.persistence.block.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.block.custom.MemberBlockCustomRepository;

import static team.startup.gwangsan.adapter.persistence.block.QMemberBlockJpaEntity.memberBlockJpaEntity;

@Repository
@RequiredArgsConstructor
public class MemberBlockCustomRepositoryImpl implements MemberBlockCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsBlockBetween(Long memberId1, Long memberId2) {
        return queryFactory
                .selectOne()
                .from(memberBlockJpaEntity)
                .where(
                        memberBlockJpaEntity.blocker.id.eq(memberId1).and(memberBlockJpaEntity.blocked.id.eq(memberId2))
                        .or(memberBlockJpaEntity.blocker.id.eq(memberId2).and(memberBlockJpaEntity.blocked.id.eq(memberId1)))
                )
                .fetchFirst() != null;
    }
}
