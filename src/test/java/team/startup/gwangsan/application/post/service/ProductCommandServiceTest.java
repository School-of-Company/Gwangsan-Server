package team.startup.gwangsan.application.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.post.data.CreateProductCommand;
import team.startup.gwangsan.application.post.data.UpdateProductCommand;
import team.startup.gwangsan.application.post.port.out.ContentModerationPort;
import team.startup.gwangsan.application.post.port.out.ProductImageAssetPort;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ObjectRequiredImageException;
import team.startup.gwangsan.domain.post.exception.ReservedProductDeletionException;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 생성·수정·삭제·검열 유스케이스")
class ProductCommandServiceTest {

    @Mock ProductPort productPort;
    @Mock ProductImagePort productImagePort;
    @Mock ProductImageAssetPort imageAssetPort;
    @Mock ContentModerationPort moderationPort;

    private static Product product(Long authorId, ProductStatus status) {
        return new Product(7L, authorId, Type.OBJECT, Mode.GIVER, "제목", "설명", 5000, status);
    }

    @Nested
    @DisplayName("생성은")
    class Describe_create {

        private CreateProductService service() {
            return new CreateProductService(productPort, productImagePort, imageAssetPort);
        }

        @Test
        @DisplayName("이미지 존재 검증 후 게시글을 저장하고 저장소가 돌려준 순서로 이미지를 연결한다")
        void it_saves_product_then_links_images() {
            when(imageAssetPort.requireExisting(List.of(12L, 11L, 12L))).thenReturn(List.of(11L, 12L));
            when(productPort.save(any())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                return new Product(30L, p.getAuthorId(), p.getType(), p.getMode(), p.getTitle(), p.getDescription(),
                        p.getGwangsan(), p.getStatus());
            });

            service().execute(new CreateProductCommand(10L, Type.OBJECT, Mode.GIVER, "제목", "설명", 5000,
                    List.of(12L, 11L, 12L)));

            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
            InOrder order = inOrder(imageAssetPort, productPort, productImagePort);
            order.verify(imageAssetPort).requireExisting(List.of(12L, 11L, 12L));
            order.verify(productPort).save(saved.capture());
            order.verify(productImagePort).addImages(30L, List.of(11L, 12L));
            assertThat(saved.getValue().getAuthorId()).isEqualTo(10L);
            assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.ONGOING);
        }

        @Test
        @DisplayName("OBJECT+GIVER 에 이미지가 없으면 아무 것도 조회·저장하지 않는다")
        void it_rejects_object_giver_without_images() {
            assertThatThrownBy(() -> service().execute(new CreateProductCommand(10L, Type.OBJECT, Mode.GIVER,
                    "제목", "설명", 5000, null))).isInstanceOf(ObjectRequiredImageException.class);
            verifyNoInteractions(imageAssetPort, productPort, productImagePort);
        }

        @Test
        @DisplayName("SERVICE 는 null 이미지를 빈 목록으로 다룬다")
        void it_treats_null_images_as_empty() {
            when(imageAssetPort.requireExisting(List.of())).thenReturn(List.of());
            when(productPort.save(any())).thenReturn(product(10L, ProductStatus.ONGOING));

            service().execute(new CreateProductCommand(10L, Type.SERVICE, Mode.RECEIVER, "제목", "설명", 5000, null));

            verify(productImagePort).addImages(7L, List.of());
        }
    }

    @Nested
    @DisplayName("수정은")
    class Describe_update {

        private UpdateProductService service() {
            return new UpdateProductService(productPort, productImagePort, imageAssetPort);
        }

        @Test
        @DisplayName("작성자의 게시글을 ONGOING 으로 저장하고 이미지 교체 후 끊긴 이미지 정리를 요청한다")
        void it_updates_and_requests_cleanup() {
            Product product = product(10L, ProductStatus.RESERVATION);
            when(productPort.findActive(7L)).thenReturn(Optional.of(product));
            when(productImagePort.replaceImages(7L, List.of(4L))).thenReturn(Set.of(3L));

            service().execute(new UpdateProductCommand(10L, 7L, Type.SERVICE, Mode.RECEIVER, "새 제목", "새 설명",
                    10000, List.of(4L)));

            InOrder order = inOrder(imageAssetPort, productPort, productImagePort);
            order.verify(imageAssetPort).requireExisting(List.of(4L));
            order.verify(productPort).save(product);
            order.verify(productImagePort).replaceImages(7L, List.of(4L));
            order.verify(imageAssetPort).requestUnusedCleanup(7L, Set.of(3L));
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ONGOING);
            assertThat(product.getTitle()).isEqualTo("새 제목");
        }

        @Test
        @DisplayName("없는 게시글은 NotFoundProductException, 비작성자는 ForbiddenProductException 이며 저장하지 않는다")
        void it_rejects_missing_or_foreign_product() {
            when(productPort.findActive(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(new UpdateProductCommand(10L, 7L, Type.SERVICE, Mode.GIVER,
                    "t", "d", 1, List.of()))).isInstanceOf(NotFoundProductException.class);

            when(productPort.findActive(7L)).thenReturn(Optional.of(product(99L, ProductStatus.ONGOING)));
            assertThatThrownBy(() -> service().execute(new UpdateProductCommand(10L, 7L, Type.SERVICE, Mode.GIVER,
                    "t", "d", 1, List.of()))).isInstanceOf(ForbiddenProductException.class);

            verify(productPort, never()).save(any());
            verifyNoInteractions(productImagePort, imageAssetPort);
        }
    }

    @Nested
    @DisplayName("삭제는")
    class Describe_delete {

        private DeleteProductService service() {
            return new DeleteProductService(productPort);
        }

        @Test
        @DisplayName("잠근 게시글을 DELETED 로 저장한다")
        void it_locks_and_marks_deleted() {
            Product product = product(10L, ProductStatus.COMPLETED);
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product));

            service().execute(10L, 7L);

            verify(productPort).save(product);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
        }

        @Test
        @DisplayName("예약 중이거나 없거나 비작성자면 저장하지 않는다")
        void it_rejects_without_saving() {
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(10L, ProductStatus.RESERVATION)));
            assertThatThrownBy(() -> service().execute(10L, 7L)).isInstanceOf(ReservedProductDeletionException.class);
            assertThatThrownBy(() -> service().execute(99L, 7L)).isInstanceOf(ForbiddenProductException.class);
            when(productPort.lockActive(8L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().execute(10L, 8L)).isInstanceOf(NotFoundProductException.class);
            verify(productPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("검열은")
    class Describe_moderation {

        @Test
        @DisplayName("제목과 내용을 줄바꿈으로 이어 검사하고 부적절하면 InappropriateContentException")
        void it_joins_title_and_description() {
            ValidateProductContentService service = new ValidateProductContentService(moderationPort);
            when(moderationPort.containsProfanity("금지\n내용")).thenReturn(true);
            when(moderationPort.containsProfanity("정상\n내용")).thenReturn(false);

            assertThatThrownBy(() -> service.execute("금지", "내용")).isInstanceOf(InappropriateContentException.class);
            service.execute("정상", "내용");
        }
    }
}
