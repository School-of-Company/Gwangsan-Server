package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.AdminImage;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.admin.port.out.AdminProductPort;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
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


/**
 * 관리자 상품 목록(3 쿼리)·논리 삭제.
 * 게시글·이미지는 post(PR06) 공개 Port 를 쓴다.
 * 작성자 닉네임·지점·빛은 member(PR02) 공개 요약 query(회원 기준 left join 1 쿼리)를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class AdminProductBridge implements AdminProductPort {

    private final ProductSnapshotQuery productSnapshotQuery;
    private final ProductImageQuery productImageQuery;
    private final ProductStatusUseCase productStatusUseCase;
    private final MemberSummaryQuery memberSummaryQuery;

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

    // 기존 계약: 상세가 없으면 light=1·placeName=null, 닉네임은 그대로. 회원 기준 left join 요약 1 쿼리.
    private Map<Long, AdminProduct.Owner> loadOwners(List<Long> memberIds) {
        Map<Long, AdminProduct.Owner> owners = new HashMap<>();
        for (MemberSummary member : memberSummaryQuery.findAllByIds(memberIds)) {
            boolean hasDetail = member.hasDetail();
            owners.put(member.memberId(), new AdminProduct.Owner(member.memberId(), member.nickname(),
                    hasDetail ? member.placeName() : null, hasDetail ? Math.max(1, member.light() / 10) : 1));
        }
        return owners;
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
