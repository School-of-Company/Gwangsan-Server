package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.port.in.DeleteProductUseCase;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.model.Product;

@Service
@RequiredArgsConstructor
public class DeleteProductService implements DeleteProductUseCase {

    private final ProductPort productPort;

    @Override
    @Transactional
    public void execute(Long actorId, Long productId) {
        // 예약 생성과 같은 행을 잠가 "예약 중 삭제 금지" 검사와 상태 변경을 직렬화한다.
        Product product = productPort.lockActive(productId)
                .orElseThrow(NotFoundProductException::new);
        product.delete(actorId);
        productPort.save(product);
    }
}
