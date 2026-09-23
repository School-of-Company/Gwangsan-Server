package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.exception.ReservedProductDeletionException;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteProductByIdServiceImpl 단위 테스트")
class DeleteProductByIdServiceImplTest {

    @InjectMocks
    private DeleteProductByIdServiceImpl deleteProductByIdService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberUtil memberUtil;

    private Member author;
    private Member otherUser;
    private Product product;

    @BeforeEach
    void setUp() {
        author = mock(Member.class);
        otherUser = mock(Member.class);
        product = mock(Product.class);

        lenient().when(author.getId()).thenReturn(1L);
        lenient().when(otherUser.getId()).thenReturn(2L);
        lenient().when(product.getMember()).thenReturn(author);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("작성자도 예약 중인 게시글은 삭제할 수 없으며 상태를 변경하지 않는다")
        void it_rejects_reserved_product_without_mutation() {
            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
            when(product.getStatus()).thenReturn(ProductStatus.RESERVATION);

            ReservedProductDeletionException exception = assertThrows(ReservedProductDeletionException.class,
                    () -> deleteProductByIdService.execute(1L));
            assertEquals(ErrorCode.RESERVED_PRODUCT_DELETION, exception.getErrorCode());
            assertEquals(409, exception.getErrorCode().getStatus());

            verify(product, never()).updateStatus(any());
            verify(productRepository, never()).delete(any());
        }

        @Nested
        @DisplayName("상품 작성자가 삭제를 요청하면")
        class Context_with_author {

            @Test
            @DisplayName("물리 삭제하지 않고 상태를 DELETED 로 변경한다")
            void it_marks_product_as_deleted() {
                // given
                Long productId = 1L;
                when(memberUtil.getCurrentMember()).thenReturn(author);
                when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

                when(product.getStatus()).thenReturn(ProductStatus.ONGOING);

                // when
                deleteProductByIdService.execute(productId);

                // then
                verify(product).updateStatus(ProductStatus.DELETED);
                verify(productRepository, never()).delete(any());
            }
        }

        @Nested
        @DisplayName("작성자가 아닌 사용자가 삭제를 요청하면")
        class Context_with_non_author {

            @Test
            @DisplayName("ForbiddenProductException 을 던진다")
            void it_throws_ForbiddenProductException() {
                // given
                Long productId = 1L;
                when(memberUtil.getCurrentMember()).thenReturn(otherUser);
                when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

                // when & then
                assertThrows(ForbiddenProductException.class,
                        () -> deleteProductByIdService.execute(productId));

                verify(product, never()).updateStatus(any());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 상품에 대해 삭제를 요청하면")
        class Context_with_nonexistent_product {

            @Test
            @DisplayName("NotFoundProductException 을 던진다")
            void it_throws_NotFoundProductException() {
                // given
                Long productId = 99L;
                when(memberUtil.getCurrentMember()).thenReturn(author);
                when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.empty());

                // when & then
                assertThrows(NotFoundProductException.class,
                        () -> deleteProductByIdService.execute(productId));

                verify(product, never()).updateStatus(any());
            }
        }
    }
}
