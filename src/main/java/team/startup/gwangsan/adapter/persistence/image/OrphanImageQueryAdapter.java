package team.startup.gwangsan.adapter.persistence.image;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.application.image.port.out.LoadOrphanImageCandidatesPort;
import team.startup.gwangsan.domain.image.model.Image;

import java.util.List;

import static team.startup.gwangsan.adapter.persistence.chat.QChatMessageImageJpaEntity.chatMessageImageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.image.QImageJpaEntity.imageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.notice.QNoticeImageJpaEntity.noticeImageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.post.QProductImage.productImage;
import static team.startup.gwangsan.adapter.persistence.report.QReportImageJpaEntity.reportImageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.feedback.QFeedbackImageJpaEntity.feedbackImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.QTradeCancelImageJpaEntity;

/**
 * 기존 단일 join SQL·정렬·limit 를 그대로 유지한다. 타 업무 Q 타입 참조는 persistence 저장 모델 간 기존 association 의
 * 기술 예외이며 core(domain/application)에는 노출하지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class OrphanImageQueryAdapter implements LoadOrphanImageCandidatesPort {

    private static final QTradeCancelImageJpaEntity tradeCancelImage = QTradeCancelImageJpaEntity.tradeCancelImageJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Image> findOrphanCandidates(int limit) {
        return queryFactory
                .selectFrom(imageJpaEntity)
                .leftJoin(productImage).on(productImage.image.eq(imageJpaEntity))
                .leftJoin(noticeImageJpaEntity).on(noticeImageJpaEntity.image.eq(imageJpaEntity))
                .leftJoin(reportImageJpaEntity).on(reportImageJpaEntity.image.eq(imageJpaEntity))
                .leftJoin(feedbackImageJpaEntity).on(feedbackImageJpaEntity.image.eq(imageJpaEntity))
                .leftJoin(chatMessageImageJpaEntity).on(chatMessageImageJpaEntity.image.eq(imageJpaEntity))
                .leftJoin(tradeCancelImage).on(tradeCancelImage.image.eq(imageJpaEntity))
                .where(
                        productImage.id.isNull(),
                        noticeImageJpaEntity.id.isNull(),
                        reportImageJpaEntity.id.isNull(),
                        feedbackImageJpaEntity.id.isNull(),
                        chatMessageImageJpaEntity.id.isNull(),
                        tradeCancelImage.id.isNull()
                )
                .orderBy(imageJpaEntity.createdAt.asc(), imageJpaEntity.id.asc())
                .limit(limit)
                .fetch()
                .stream()
                .map(ImagePersistenceAdapter::toModel)
                .toList();
    }
}
