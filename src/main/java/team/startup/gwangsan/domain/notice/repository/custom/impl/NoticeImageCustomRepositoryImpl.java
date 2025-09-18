package team.startup.gwangsan.domain.notice.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.repository.custom.NoticeImageCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.notice.entity.QNoticeImage.noticeImage;

@Repository
@RequiredArgsConstructor
public class NoticeImageCustomRepositoryImpl implements NoticeImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NoticeImage> findAllByNotice(Notice notice) {
        return queryFactory
                .selectFrom(noticeImage)
                .join(noticeImage.image).fetchJoin()
                .where(noticeImage.notice.eq(notice))
                .fetch();
    }

    @Override
    public List<NoticeImage> findAllByNoticeIdIn(List<Long> noticeIds) {
        return queryFactory
                .selectFrom(noticeImage)
                .join(noticeImage.image).fetchJoin()
                .where(noticeImage.id.in(noticeIds))
                .fetch();
    }
}
