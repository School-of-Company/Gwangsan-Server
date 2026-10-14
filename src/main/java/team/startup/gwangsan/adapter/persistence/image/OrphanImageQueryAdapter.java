package team.startup.gwangsan.adapter.persistence.image;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.application.image.port.out.LoadOrphanImageCandidatesPort;
import team.startup.gwangsan.domain.image.model.Image;

import java.util.List;

import static team.startup.gwangsan.domain.chat.entity.QChatMessageImage.chatMessageImage;
import static team.startup.gwangsan.domain.image.entity.QImage.image;
import static team.startup.gwangsan.domain.notice.entity.QNoticeImage.noticeImage;
import static team.startup.gwangsan.adapter.persistence.post.QProductImage.productImage;
import static team.startup.gwangsan.domain.report.entity.QReportImage.reportImage;
import static team.startup.gwangsan.domain.trade.entity.QTradeCancelImage.tradeCancelImage;

/**
 * 기존 단일 join SQL·정렬·limit 를 그대로 유지한다. 타 업무 Q 타입 참조는 persistence 저장 모델 간 기존 association 의
 * 기술 예외이며 core(domain/application)에는 노출하지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class OrphanImageQueryAdapter implements LoadOrphanImageCandidatesPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Image> findOrphanCandidates(int limit) {
        return queryFactory
                .selectFrom(image)
                .leftJoin(productImage).on(productImage.image.eq(image))
                .leftJoin(noticeImage).on(noticeImage.image.eq(image))
                .leftJoin(reportImage).on(reportImage.image.eq(image))
                .leftJoin(chatMessageImage).on(chatMessageImage.image.eq(image))
                .leftJoin(tradeCancelImage).on(tradeCancelImage.image.eq(image))
                .where(
                        productImage.id.isNull(),
                        noticeImage.id.isNull(),
                        reportImage.id.isNull(),
                        chatMessageImage.id.isNull(),
                        tradeCancelImage.id.isNull()
                )
                .orderBy(image.createdAt.asc(), image.id.asc())
                .limit(limit)
                .fetch()
                .stream()
                .map(ImagePersistenceAdapter::toModel)
                .toList();
    }
}
