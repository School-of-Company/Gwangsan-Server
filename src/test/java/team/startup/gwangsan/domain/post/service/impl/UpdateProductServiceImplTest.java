package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ObjectRequiredImageException;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.event.DeleteNotUsedImageEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProductServiceImplTest 단위 테스트")
class UpdateProductServiceImplTest {

    @InjectMocks
    private UpdateProductServiceImpl updateProductService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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
        @DisplayName("OBJECT+GIVER 타입에 이미지가 없으면 ObjectRequiredImageException을 던진다")
        void givenObjectGiverWithNoImages_whenUpdateProduct_thenThrowsObjectRequiredException() {
            Long productId = 1L;
            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            assertThrows(ObjectRequiredImageException.class, () ->
                    updateProductService.execute(productId, Type.OBJECT, Mode.GIVER, "T", "D", 100, Collections.emptyList()));
        }

        @Test
        @DisplayName("상품 작성자가 요청하면 상품 정보가 업데이트된다")
        void givenAuthor_whenUpdateProduct_thenProductIsUpdated() {
            // given
            Long productId = 1L;
            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            ProductImage pi1 = mock(ProductImage.class);
            ProductImage pi2 = mock(ProductImage.class);

            Image old1 = mock(Image.class);
            Image old2 = mock(Image.class);
            when(old1.getId()).thenReturn(10L);
            when(old2.getId()).thenReturn(20L);
            when(pi1.getImage()).thenReturn(old1);
            when(pi2.getImage()).thenReturn(old2);

            when(productImageRepository.findAllByProductId(productId))
                    .thenReturn(List.of(pi1, pi2));

            List<Long> requestImageIds = List.of(20L, 30L);

            Image newImage = mock(Image.class);
            when(newImage.getId()).thenReturn(30L);

            when(imageRepository.findByIdIn(requestImageIds))
                    .thenReturn(List.of(old2, newImage));

            // when
            updateProductService.execute(
                    productId,
                    Type.SERVICE,
                    Mode.RECEIVER,
                    "updated title",
                    "updated desc",
                    200,
                    requestImageIds
            );

            // then
            verify(product).update(Type.SERVICE, Mode.RECEIVER, "updated title", "updated desc", 200);
            verify(productImageRepository).deleteAllInBatch(anyList());
            verify(productImageRepository).saveAll(anyList());
            verify(applicationEventPublisher).publishEvent(any(DeleteNotUsedImageEvent.class));
        }

        @Test
        @DisplayName("작성자가 아니면 ForbiddenProductException 을 던진다")
        void givenNonAuthor_whenUpdateProduct_thenThrowsForbidden() {
            // given
            Long productId = 1L;

            when(memberUtil.getCurrentMember()).thenReturn(otherUser);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // when & then
            assertThrows(ForbiddenProductException.class, () ->
                    updateProductService.execute(
                            productId,
                            Type.OBJECT,
                            Mode.GIVER,
                            "T",
                            "D",
                            1,
                            Collections.emptyList()
                    ));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NotFoundProductException 을 던진다")
        void givenNonExistentProduct_whenUpdateProduct_thenThrowsNotFound() {
            // given
            Long productId = 99L;

            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundProductException.class, () ->
                    updateProductService.execute(
                            productId,
                            Type.OBJECT,
                            Mode.GIVER,
                            "T",
                            "D",
                            1,
                            Collections.emptyList()
                    ));
        }

        @Test
        @DisplayName("새로운 이미지가 추가되지 않은 경우 저장 메서드를 호출하지 않는다")
        void givenNoNewImages_whenUpdateProduct_thenNoSaveImageCall() {
            // given
            Long productId = 1L;
            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            ProductImage pi = mock(ProductImage.class);
            Image img = mock(Image.class);
            when(img.getId()).thenReturn(10L);
            when(pi.getImage()).thenReturn(img);

            when(productImageRepository.findAllByProductId(productId))
                    .thenReturn(List.of(pi));

            List<Long> requestImageIds = List.of(10L);

            when(imageRepository.findByIdIn(requestImageIds))
                    .thenReturn(List.of(img));

            // when
            updateProductService.execute(
                    productId,
                    Type.OBJECT,
                    Mode.GIVER,
                    "A",
                    "B",
                    10,
                    requestImageIds
            );

            // then
            verify(productImageRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("요청 이미지 중 기존 이미지가 없으면 예외 발생 없이 정상 처리된다")
        void givenPartialMatchImages_whenUpdateProduct_thenWorks() {
            // given
            Long productId = 1L;

            when(memberUtil.getCurrentMember()).thenReturn(author);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            ProductImage pi = mock(ProductImage.class);
            Image existing = mock(Image.class);
            when(existing.getId()).thenReturn(10L);
            when(pi.getImage()).thenReturn(existing);

            when(productImageRepository.findAllByProductId(productId))
                    .thenReturn(List.of(pi));

            Image newImg = mock(Image.class);
            when(newImg.getId()).thenReturn(20L);

            when(imageRepository.findByIdIn(List.of(10L, 20L)))
                    .thenReturn(List.of(existing, newImg));

            // when & then
            assertDoesNotThrow(() ->
                    updateProductService.execute(
                            productId,
                            Type.SERVICE,
                            Mode.RECEIVER,
                            "T",
                            "D",
                            10,
                            List.of(10L, 20L)
                    ));
        }
    }

    @Test
    @DisplayName("새로운 이미지가 추가되지 않은 경우에도 상품 정보 업데이트는 수행하고, 이미지 saveAll 은 호출되지 않는다")
    void givenNoNewImages_whenUpdateProduct_thenProductUpdatedButNoImageSave() {
        Long productId = 1L;

        when(memberUtil.getCurrentMember()).thenReturn(author);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductImage pi = mock(ProductImage.class);
        Image existingImg = mock(Image.class);
        when(existingImg.getId()).thenReturn(10L);
        when(pi.getImage()).thenReturn(existingImg);

        when(productImageRepository.findAllByProductId(productId))
                .thenReturn(List.of(pi));

        List<Long> requestImageIds = List.of(10L);

        when(imageRepository.findByIdIn(requestImageIds))
                .thenReturn(List.of(existingImg));

        updateProductService.execute(
                productId,
                Type.OBJECT,
                Mode.GIVER,
                "A",
                "B",
                10,
                requestImageIds
        );

        verify(product).update(Type.OBJECT, Mode.GIVER, "A", "B", 10);

        verify(productImageRepository, never()).saveAll(any());

        verify(productImageRepository, never()).deleteAllInBatch(any());
    }
}