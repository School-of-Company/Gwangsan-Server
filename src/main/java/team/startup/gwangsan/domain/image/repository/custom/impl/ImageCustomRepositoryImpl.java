package team.startup.gwangsan.domain.image.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.custom.ImageCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.image.entity.QImage.image;
import static team.startup.gwangsan.domain.notice.entity.QNoticeImage.noticeImage;
import static team.startup.gwangsan.domain.post.entity.QProductImage.productImage;
import static team.startup.gwangsan.domain.report.entity.QReportImage.reportImage;

@Repository
@RequiredArgsConstructor
public class ImageCustomRepositoryImpl implements ImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Image> findAllOrphanImages(int limit) {
        return queryFactory
                .selectFrom(image)
                .leftJoin(productImage).on(productImage.image.eq(image))
                .leftJoin(noticeImage).on(noticeImage.image.eq(image))
                .leftJoin(reportImage).on(reportImage.image.eq(image))
                .where(
                        productImage.id.isNull(),
                        noticeImage.id.isNull(),
                        reportImage.id.isNull()
                )
                .orderBy(image.createdAt.asc(), image.id.asc())
                .limit(limit)
                .fetch();

    }
}
