package team.startup.gwangsan.adapter.persistence.feedback;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.List;
import java.util.Optional;

import static team.startup.gwangsan.adapter.persistence.feedback.QFeedbackJpaEntity.feedbackJpaEntity;

/**
 * Member → MemberDetail 는 참조 방향이 없어(MemberDetail 만 Member 를 참조) fetch join 대상이 될 수 없다.
 * 그래서 작성자 닉네임·지점은 별도 컬럼으로 projection 하고, 기본 필드는 FeedbackJpaEntity 를 그대로 담는다.
 */
@Repository
@RequiredArgsConstructor
public class FeedbackCustomRepositoryImpl implements FeedbackCustomRepository {

    private static final QFeedbackJpaEntity feedback = feedbackJpaEntity;
    private static final QMemberJpaEntity author = new QMemberJpaEntity("author");
    private static final QMemberJpaEntity answeredByMember = new QMemberJpaEntity("answeredByMember");
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FeedbackJpaEntity> findMyFeedbacks(Long memberId, Long lastId, int size) {
        return queryFactory.selectFrom(feedback)
                .where(feedback.member.id.eq(memberId), lastId != null ? feedback.id.lt(lastId) : null)
                .orderBy(feedback.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Tuple> findAdminRows(FeedbackStatus status, FeedbackType type, Integer strictPlaceId,
                                      Integer scopePlaceIdExemptingBug, Long lastId, int size) {
        return rowQuery()
                .where(
                        statusEq(status),
                        typeEq(type),
                        strictPlaceId != null ? memberDetail.place.id.eq(strictPlaceId) : null,
                        scopeOrBug(scopePlaceIdExemptingBug),
                        lastId != null ? feedback.id.lt(lastId) : null
                )
                .orderBy(feedback.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public Optional<Tuple> findRowById(Long feedbackId) {
        return Optional.ofNullable(rowQuery().where(feedback.id.eq(feedbackId)).fetchOne());
    }

    @Override
    public Optional<Tuple> findAccessRow(Long feedbackId) {
        return Optional.ofNullable(queryFactory
                .select(feedback.type, memberDetail.place.id)
                .from(feedback)
                .join(memberDetail).on(memberDetail.member.id.eq(feedback.member.id))
                .where(feedback.id.eq(feedbackId))
                .fetchOne());
    }

    private JPAQuery<Tuple> rowQuery() {
        return queryFactory
                .select(feedback, author.nickname, memberDetail.place.id, memberDetail.place.name,
                        answeredByMember.nickname)
                .from(feedback)
                .join(feedback.member, author)
                .join(memberDetail).on(memberDetail.member.id.eq(author.id))
                .leftJoin(feedback.answeredBy, answeredByMember);
    }

    private BooleanExpression statusEq(FeedbackStatus status) {
        return status != null ? feedback.status.eq(status) : null;
    }

    private BooleanExpression typeEq(FeedbackType type) {
        return type != null ? feedback.type.eq(type) : null;
    }

    /** BUG 는 지점과 무관하게, 나머지 유형은 이 지점에 속한 작성자만. */
    private BooleanExpression scopeOrBug(Integer placeId) {
        return placeId != null ? feedback.type.eq(FeedbackType.BUG).or(memberDetail.place.id.eq(placeId)) : null;
    }
}
