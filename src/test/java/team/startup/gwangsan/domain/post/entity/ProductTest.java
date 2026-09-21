package team.startup.gwangsan.domain.post.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product 단위 테스트")
class ProductTest {

    @Test
    @DisplayName("예약된 게시물을 수정하면 내용과 거래 조건을 갱신하고 ONGOING으로 되돌린다")
    void it_resets_status_to_ongoing_when_updating_reserved_product() {
        Product product = Product.builder()
                .title("기존 제목")
                .description("기존 설명")
                .gwangsan(100)
                .type(Type.OBJECT)
                .mode(Mode.GIVER)
                .status(ProductStatus.RESERVATION)
                .build();

        product.update(Type.SERVICE, Mode.RECEIVER, "새 제목", "새 설명", 200);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
        assertThat(product.getTitle()).isEqualTo("새 제목");
        assertThat(product.getDescription()).isEqualTo("새 설명");
        assertThat(product.getGwangsan()).isEqualTo(200);
        assertThat(product.getType()).isEqualTo(Type.SERVICE);
        assertThat(product.getMode()).isEqualTo(Mode.RECEIVER);
    }
}
