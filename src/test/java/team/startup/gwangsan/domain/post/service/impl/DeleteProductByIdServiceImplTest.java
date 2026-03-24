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
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

        @Nested
        @DisplayName("상품 작성자가 삭제를 요청하면")
        class Context_with_author {

            @Test
            @DisplayName("정상적으로 상품을 삭제한다")
            void it_deletes_product() {
                // given
                Long productId = 1L;
                when(memberUtil.getCurrentMember()).thenReturn(author);
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));

                // when
                deleteProductByIdService.execute(productId);

                // then
                verify(productRepository).delete(product);
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
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));

                // when & then
                assertThrows(ForbiddenProductException.class,
                        () -> deleteProductByIdService.execute(productId));

                verify(productRepository, never()).delete(any());
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
                when(productRepository.findById(productId)).thenReturn(Optional.empty());

                // when & then
                assertThrows(NotFoundProductException.class,
                        () -> deleteProductByIdService.execute(productId));

                verify(productRepository, never()).delete(any());
            }
        }
    }
}