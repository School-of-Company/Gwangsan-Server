package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.custom.ProductCustomRepository;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.List;

import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.post.entity.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(Type type, Mode mode, Place place, ProductStatus status) {
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
    public List<Product> findProductByMemberAndTypeAndModeAndStatus(Member member, Type type, Mode mode, ProductStatus status) {
        return queryFactory
                .selectFrom(product).distinct()
                .where(
                        product.member.id.eq(member.getId()),
                        typeEq(type),
                        modeEq(mode),
                        statusEq(status)
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
}
