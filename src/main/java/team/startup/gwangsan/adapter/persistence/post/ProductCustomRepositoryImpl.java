package team.startup.gwangsan.adapter.persistence.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.Collection;
import java.util.List;

import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import static team.startup.gwangsan.adapter.persistence.post.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {
    private static final QMemberJpaEntity member = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> findMemberProducts(Long memberId, Type type, Mode mode, Long lastId, int size, Boolean completed) {
        List<ProductStatus> statuses = completed == null
                ? List.of(ProductStatus.ONGOING, ProductStatus.RESERVATION, ProductStatus.COMPLETED)
                : completed ? List.of(ProductStatus.COMPLETED)
                : List.of(ProductStatus.ONGOING, ProductStatus.RESERVATION);
        return queryFactory.selectFrom(product)
                .where(
                        product.member.id.eq(memberId),
                        typeEq(type),
                        modeEq(mode),
                        product.status.in(statuses),
                        lastId != null ? product.id.lt(lastId) : null
                )
                .orderBy(product.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Product> findAdminProducts(Type type, Mode mode, Long lastId, int size) {
        return queryFactory.selectFrom(product)
                .join(product.member, member).fetchJoin()
                .where(
                        typeEq(type),
                        modeEq(mode),
                        product.status.ne(ProductStatus.DELETED),
                        lastId != null ? product.id.lt(lastId) : null
                )
                .orderBy(product.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Product> findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(Type type, Mode mode, PlaceJpaEntity place, ProductStatus status) {
        return queryFactory
                .selectFrom(product).distinct()
                .join(product.member, member).fetchJoin()
                .join(memberDetail).on(memberDetail.member.id.eq(member.id)).fetchJoin()
                .where(
                        typeEq(type),
                        modeEq(mode),
                        memberDetail.place.eq(place),
                        statusEq(status)
                )
                .fetch()
                .stream()
                .toList();
    }

    @Override
    public List<Product> findProductByMemberAndTypeAndModeAndStatusIn(MemberJpaEntity member, Type type, Mode mode, Collection<ProductStatus> statuses) {
        return queryFactory
                .selectFrom(product).distinct()
                .where(
                        product.member.id.eq(member.getId()),
                        typeEq(type),
                        modeEq(mode),
                        statusIn(statuses)
                )
                .fetch()
                .stream()
                .toList();
    }

    private BooleanExpression typeEq(Type type) {
        return type != null ? product.type.eq(type) : null;
    }

    private BooleanExpression modeEq(Mode mode) {
        return mode != null ? product.mode.eq(mode) : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }

    private BooleanExpression statusIn(Collection<ProductStatus> statuses) {
        return (statuses == null || statuses.isEmpty()) ? null : product.status.in(statuses);
    }
}
