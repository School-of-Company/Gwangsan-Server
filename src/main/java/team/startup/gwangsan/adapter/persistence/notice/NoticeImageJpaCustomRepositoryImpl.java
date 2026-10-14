package team.startup.gwangsan.adapter.persistence.notice;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static team.startup.gwangsan.adapter.persistence.notice.QNoticeImageJpaEntity.noticeImageJpaEntity;

@Repository
@RequiredArgsConstructor
public class NoticeImageJpaCustomRepositoryImpl implements NoticeImageJpaCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NoticeImageJpaEntity> findAllByNoticeId(Long noticeId) {
        return queryFactory
                .selectFrom(noticeImageJpaEntity)
                .join(noticeImageJpaEntity.image).fetchJoin()
                .where(noticeImageJpaEntity.notice.id.eq(noticeId))
                .fetch();
    }

    @Override
    public List<NoticeImageJpaEntity> findAllByNoticeIdIn(List<Long> noticeIds) {
        return queryFactory
                .selectFrom(noticeImageJpaEntity)
                .join(noticeImageJpaEntity.image).fetchJoin()
                .where(noticeImageJpaEntity.notice.id.in(noticeIds))
                .fetch();
    }
}
