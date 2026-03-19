package team.startup.gwangsan.domain.block.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.block.repository.custom.MemberBlockCustomRepository;

import static team.startup.gwangsan.domain.block.entity.QMemberBlock.memberBlock;

@Repository
@RequiredArgsConstructor
public class MemberBlockCustomRepositoryImpl implements MemberBlockCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsBlockBetween(Long memberId1, Long memberId2) {
        return queryFactory
                .selectOne()
                .from(memberBlock)
                .where(
                        memberBlock.blocker.id.eq(memberId1).and(memberBlock.blocked.id.eq(memberId2))
                        .or(memberBlock.blocker.id.eq(memberId2).and(memberBlock.blocked.id.eq(memberId1)))
                )
                .fetchFirst() != null;
    }
}
