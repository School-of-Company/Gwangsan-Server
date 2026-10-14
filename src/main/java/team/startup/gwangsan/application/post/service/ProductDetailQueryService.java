package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ProductAuthor;
import team.startup.gwangsan.application.post.data.ProductDetailView;
import team.startup.gwangsan.application.post.port.in.ProductDetailQuery;
import team.startup.gwangsan.application.post.port.out.BlockPolicyPort;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductPlaceMismatchException;
import team.startup.gwangsan.domain.post.model.Product;

@Service
@RequiredArgsConstructor
public class ProductDetailQueryService implements ProductDetailQuery {

    private final ProductPort productPort;
    private final ProductImagePort productImagePort;
    private final ProductMemberPort memberPort;
    private final BlockPolicyPort blockPolicyPort;
    private final ProductChatPort chatPort;

    // 검사 순서(내 지점 → 게시글 → 차단 → 작성자 지점·정보 → 같은 지점)는 기존 오류 우선순위다.
    @Override
    @Transactional(readOnly = true)
    public ProductDetailView query(Long actorId, Long productId) {
        Integer viewerPlaceId = memberPort.placeIdOf(actorId);
        Product product = productPort.findActive(productId)
                .orElseThrow(NotFoundProductException::new);
        blockPolicyPort.validateNotBlocked(actorId, product.getAuthorId());
        Integer authorPlaceId = memberPort.placeIdOf(product.getAuthorId());
        ProductAuthor author = ProductAuthor.of(memberPort.profile(product.getAuthorId()));
        if (!authorPlaceId.equals(viewerPlaceId)) {
            throw new ProductPlaceMismatchException();
        }

        boolean completable = chatPort.findRoom(product.getId(), actorId)
                .map(room -> chatPort.hasSentMessage(room.roomId(), actorId))
                .orElse(false);
        return new ProductDetailView(product.getId(), product.getTitle(), product.getDescription(),
                product.getGwangsan(), product.getType(), product.getMode(), author,
                productImagePort.findImages(product.getId()), product.isAuthor(actorId), completable,
                product.isCompleted(), product.isReserved());
    }
}
