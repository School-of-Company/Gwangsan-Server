package team.startup.gwangsan.adapter.persistence.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.adapter.persistence.post.QProductImage.productImage;

@Repository
@RequiredArgsConstructor
public class ProductImageCustomRepositoryImpl implements ProductImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProductImage> findAllByProductIdIn(Collection<Long> productIds) {
        return queryFactory
                .selectFrom(productImage)
                .join(productImage.image).fetchJoin()
                .join(productImage.product).fetchJoin()
                .where(productImage.product.id.in(productIds))
                .fetch();
    }

    @Override
    public List<ProductImage> findAllByProductId(Long productId) {
        return queryFactory
                .selectFrom(productImage)
                .join(productImage.image).fetchJoin()
                .join(productImage.product).fetchJoin()
                .where(productImage.product.id.eq(productId))
                .fetch();
    }
}
