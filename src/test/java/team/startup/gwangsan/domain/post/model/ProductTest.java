package team.startup.gwangsan.domain.post.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.ObjectRequiredImageException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.exception.ReservedProductDeletionException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("순수 게시글 모델")
class ProductTest {

    private static Product product(ProductStatus status) {
        return new Product(1L, 10L, Type.SERVICE, Mode.GIVER, "제목", "설명", 5000, status);
    }

    @Nested
    @DisplayName("create 는")
    class Describe_create {

        @Test
        @DisplayName("OBJECT+GIVER 에 이미지가 없으면 ObjectRequiredImageException")
        void it_requires_image_for_object_giver() {
            assertThatThrownBy(() -> Product.create(10L, Type.OBJECT, Mode.GIVER, "t", "d", 1, List.of()))
                    .isInstanceOf(ObjectRequiredImageException.class);
            assertThatThrownBy(() -> Product.create(10L, Type.OBJECT, Mode.GIVER, "t", "d", 1, null))
                    .isInstanceOf(ObjectRequiredImageException.class);
        }

        @Test
        @DisplayName("SERVICE 나 OBJECT+RECEIVER 는 이미지 없이 ONGOING 으로 만든다")
        void it_creates_ongoing_without_image_otherwise() {
            assertThat(Product.create(10L, Type.SERVICE, Mode.GIVER, "t", "d", 1, List.of()).getStatus())
                    .isEqualTo(ProductStatus.ONGOING);
            Product receiver = Product.create(10L, Type.OBJECT, Mode.RECEIVER, "t", "d", 1, List.of());
            assertThat(receiver.getStatus()).isEqualTo(ProductStatus.ONGOING);
            assertThat(receiver.getId()).isNull();
            assertThat(receiver.getAuthorId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("update 는")
    class Describe_update {

        @Test
        @DisplayName("기존 동작대로 예약·완료 상태도 ONGOING 으로 되돌린다")
        void it_resets_status_to_ongoing() {
            Product product = product(ProductStatus.RESERVATION);

            product.update(10L, Type.OBJECT, Mode.RECEIVER, "새 제목", "새 설명", 7000, List.of());

            assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
            assertThat(product.getTitle()).isEqualTo("새 제목");
            assertThat(product.getGwangsan()).isEqualTo(7000);
        }

        @Test
        @DisplayName("작성자 검사가 이미지 필수 검사보다 먼저다")
        void it_checks_author_before_image() {
            assertThatThrownBy(() -> product(ProductStatus.ONGOING)
                    .update(99L, Type.OBJECT, Mode.GIVER, "t", "d", 1, List.of()))
                    .isInstanceOf(ForbiddenProductException.class);
            assertThatThrownBy(() -> product(ProductStatus.ONGOING)
                    .update(10L, Type.OBJECT, Mode.GIVER, "t", "d", 1, List.of()))
                    .isInstanceOf(ObjectRequiredImageException.class);
        }
    }

    @Nested
    @DisplayName("delete 는")
    class Describe_delete {

        @Test
        @DisplayName("작성자의 비예약 게시글을 DELETED 로 바꾼다")
        void it_marks_deleted() {
            Product product = product(ProductStatus.COMPLETED);
            product.delete(10L);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
        }

        @Test
        @DisplayName("비작성자는 예약 상태와 무관하게 권한 오류가 우선한다")
        void it_checks_author_first() {
            Product product = product(ProductStatus.RESERVATION);
            assertThatThrownBy(() -> product.delete(99L)).isInstanceOf(ForbiddenProductException.class);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVATION);
        }

        @Test
        @DisplayName("예약 중이면 ReservedProductDeletionException 이고 상태를 바꾸지 않는다")
        void it_rejects_reserved() {
            Product product = product(ProductStatus.RESERVATION);
            assertThatThrownBy(() -> product.delete(10L)).isInstanceOf(ReservedProductDeletionException.class);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVATION);
        }
    }

    @Nested
    @DisplayName("reserve 는")
    class Describe_reserve {

        @Test
        @DisplayName("작성자 → 이미 예약 → 진행 중 아님 순서로 거절한다")
        void it_checks_in_legacy_order() {
            assertThatThrownBy(() -> product(ProductStatus.RESERVATION).reserve(99L))
                    .isInstanceOf(ForbiddenProductException.class);
            assertThatThrownBy(() -> product(ProductStatus.RESERVATION).reserve(10L))
                    .isInstanceOf(ProductAlreadyReservationException.class);
            assertThatThrownBy(() -> product(ProductStatus.COMPLETED).reserve(10L))
                    .isInstanceOf(ProductNotOngoingException.class);
        }

        @Test
        @DisplayName("진행 중이면 RESERVATION 으로 바꾸고 취소하면 ONGOING 으로 되돌린다")
        void it_reserves_and_reopens() {
            Product product = product(ProductStatus.ONGOING);
            product.reserve(10L);
            assertThat(product.isReserved()).isTrue();
            product.reopen();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("공개 상태 변경은")
    class Describe_public_transition {

        @Test
        @DisplayName("complete 는 COMPLETED, 관리자 삭제는 예약 중이어도 DELETED 로 바꾼다")
        void it_completes_and_deletes_by_admin() {
            Product completed = product(ProductStatus.ONGOING);
            completed.complete();
            assertThat(completed.isCompleted()).isTrue();

            Product reserved = product(ProductStatus.RESERVATION);
            reserved.deleteByAdmin();
            assertThat(reserved.getStatus()).isEqualTo(ProductStatus.DELETED);
        }
    }
}
