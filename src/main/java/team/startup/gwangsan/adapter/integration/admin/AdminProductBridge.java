package team.startup.gwangsan.adapter.integration.admin;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.application.admin.data.AdminImage;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.admin.port.out.AdminProductPort;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.post.port.in.ProductStatusUseCase;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static team.startup.gwangsan.domain.place.entity.QPlace.place;

/**
 * 관리자 상품 목록(3 쿼리)·논리 삭제.
 * 게시글·이미지는 post(PR06) 공개 Port 를 쓴다.
 * legacy bridge: 작성자 닉네임·지점·빛은 member(PR02) 공개 batch query 가 없어 회원 저장 모델 left join 1 쿼리로 읽는다
 * (PR02 projection 은 inner join 이라 상세 없는 작성자가 빠지므로 쓰지 않는다). 제거: member 공개 batch query → PR12.
 */
@Component
@RequiredArgsConstructor
public class AdminProductBridge implements AdminProductPort {

    private static final QMemberJpaEntity MEMBER = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity DETAIL = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;
    private final ProductStatusUseCase productStatusUseCase;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminProduct> findProducts(AdminProductSearch search) {
        List<ProductSnapshot> products = productSnapshotQuery.findActivePage(
                search.type() == null ? null : Type.valueOf(search.type()),
                search.mode() == null ? null : Mode.valueOf(search.mode()),
                search.lastId(), search.size());
        if (products.isEmpty()) {
            return List.of();
        }

        Map<Long, AdminProduct.Owner> owners = loadOwners(products.stream()
                .map(ProductSnapshot::authorId)
                .distinct()
                .toList());
        Map<Long, List<ImageView>> images = productImageQuery.findImages(products.stream()
                .map(ProductSnapshot::id)
                .toList());

        return products.stream()
                .map(product -> new AdminProduct(
                        product.id(),
                        product.title(),
                        product.description(),
                        product.gwangsan(),
                        nameOf(product.type()),
                        nameOf(product.mode()),
                        owners.get(product.authorId()),
                        images.getOrDefault(product.id(), List.of()).stream()
                                .map(image -> new AdminImage(image.imageId(), image.imageUrl()))
                                .toList(),
                        product.isCompleted(),
                        product.isReserved()))
                .toList();
    }

    @Override
    public void deleteActive(Long productId) {
        productStatusUseCase.deleteByAdmin(productId);
    }

    // 기존 계약: 상세가 없으면 light=1·placeName=null, 지점이 없으면 placeName=null, 닉네임은 그대로.
    private Map<Long, AdminProduct.Owner> loadOwners(List<Long> memberIds) {
        List<Tuple> rows = queryFactory
                .select(MEMBER.id, MEMBER.nickname, DETAIL.id, DETAIL.light, place.name)
                .from(MEMBER)
                .leftJoin(DETAIL).on(DETAIL.member.id.eq(MEMBER.id))
                .leftJoin(DETAIL.place, place)
                .where(MEMBER.id.in(memberIds))
                .fetch();
        Map<Long, AdminProduct.Owner> owners = new HashMap<>();
        for (Tuple row : rows) {
            boolean hasDetail = row.get(DETAIL.id) != null;
            int light = hasDetail ? Math.max(1, row.get(DETAIL.light) / 10) : 1;
            owners.put(row.get(MEMBER.id), new AdminProduct.Owner(row.get(MEMBER.id), row.get(MEMBER.nickname),
                    hasDetail ? row.get(place.name) : null, light));
        }
        return owners;
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
